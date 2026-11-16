// 用 CDP 驱动 Chrome 复现登录，抓取请求体/响应体/控制台
import { spawn } from 'node:child_process'
import { setTimeout as sleep } from 'node:timers/promises'
import fs from 'node:fs'

const CHROME = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const PORT = 9333
const URL = 'http://localhost:5173/login'

const child = spawn(CHROME, [
  '--headless=new', '--disable-gpu', '--no-sandbox', '--hide-scrollbars',
  `--remote-debugging-port=${PORT}`,
  '--user-data-dir=' + (process.env.TEMP + '\\cdp-profile-' + Date.now()),
  '--window-size=1440,900', 'about:blank'
], { stdio: 'ignore' })

async function getWsUrl() {
  for (let i = 0; i < 40; i++) {
    try {
      const r = await fetch(`http://127.0.0.1:${PORT}/json/list`)
      const list = await r.json()
      const page = list.find(t => t.type === 'page')
      if (page?.webSocketDebuggerUrl) return page.webSocketDebuggerUrl
    } catch { /* not ready */ }
    await sleep(300)
  }
  throw new Error('chrome not ready')
}

const ws = new WebSocket(await getWsUrl())
await new Promise(r => ws.addEventListener('open', r))

let id = 0
const pending = new Map()
const logs = []
const netLog = []

ws.addEventListener('message', ev => {
  const msg = JSON.parse(ev.data)
  if (msg.id && pending.has(msg.id)) {
    pending.get(msg.id)(msg)
    pending.delete(msg.id)
    return
  }
  if (msg.method === 'Runtime.consoleAPICalled') {
    logs.push('[console.' + msg.params.type + '] ' + msg.params.args.map(a => a.value ?? a.description ?? a.type).join(' '))
  }
  if (msg.method === 'Runtime.exceptionThrown') {
    logs.push('[exception] ' + (msg.params.exceptionDetails.exception?.description || msg.params.exceptionDetails.text))
  }
  if (msg.method === 'Network.requestWillBeSent' && msg.params.request.url.includes('/api/')) {
    netLog.push({ kind: 'REQ', url: msg.params.request.url, method: msg.params.request.method, body: msg.params.request.postData })
  }
  if (msg.method === 'Network.responseReceived' && msg.params.response.url.includes('/api/')) {
    netLog.push({ kind: 'RES', url: msg.params.response.url, status: msg.params.response.status })
  }
})

function send(method, params = {}) {
  const mid = ++id
  ws.send(JSON.stringify({ id: mid, method, params }))
  return new Promise(res => pending.set(mid, res))
}

async function evaluate(expr) {
  const r = await send('Runtime.evaluate', { expression: expr, awaitPromise: true, returnByValue: true })
  return r.result?.result?.value
}

await send('Runtime.enable')
await send('Network.enable')
await send('Page.enable')
await send('Page.navigate', { url: URL })
await sleep(3500)

// 填表（Vue v-model 需要 input 事件）
await evaluate(`
(() => {
  const inputs = [...document.querySelectorAll('input')];
  const set = (el, v) => {
    const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
    setter.call(el, v);
    el.dispatchEvent(new Event('input', { bubbles: true }));
    el.dispatchEvent(new Event('change', { bubbles: true }));
  };
  set(inputs[0], 'admin');
  set(inputs[1], 'admin123');
  return inputs.length + ' inputs filled';
})()
`)
const filled = await evaluate(`[...document.querySelectorAll('input')].map(i => i.value).join(' | ')`)
console.log('填表结果:', filled)

// 点击登录按钮
await evaluate(`
(() => {
  const btn = [...document.querySelectorAll('button')].find(b => b.innerText.replace(/\\s/g,'') === '登录');
  if (!btn) return 'no button';
  btn.click();
  return 'clicked';
})()
`)
await sleep(4000)

const state = await evaluate(`JSON.stringify({
  url: location.href,
  token: localStorage.getItem('token') ? 'HAS_TOKEN' : 'NO_TOKEN',
  msg: [...document.querySelectorAll('.el-message__content')].map(e => e.innerText),
  bodyText: document.body.innerText.slice(0, 200)
})`)
console.log('页面状态:', state)

console.log('--- 网络 ---')
netLog.forEach(l => console.log(JSON.stringify(l)))
console.log('--- 控制台 ---')
logs.slice(0, 20).forEach(l => console.log(l))

const shot = await send('Page.captureScreenshot', { format: 'png' })
fs.writeFileSync(process.env.TEMP + '\\debug-login.png', Buffer.from(shot.result.data, 'base64'))
console.log('截图:', process.env.TEMP + '\\debug-login.png')

ws.close()
child.kill()
process.exit(0)
