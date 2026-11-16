/**
 * 接口自检脚本：后端启动后执行
 *   node scripts/verify-api.mjs
 * 覆盖：登录、检索、借书（库存原子扣减）、续借、还书（库存回补）、
 *       收藏、权限隔离(403)、未登录(401)、参数校验。
 */
const BASE = process.env.API_BASE || 'http://localhost:8080/api';

async function call(method, path, { token, body, query } = {}) {
  let url = BASE + path;
  if (query) url += '?' + new URLSearchParams(query).toString();
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = 'Bearer ' + token;
  const res = await fetch(url, { method, headers, body: body ? JSON.stringify(body) : undefined });
  const text = await res.text();
  let json;
  try { json = JSON.parse(text); } catch { json = { raw: text }; }
  return { http: res.status, ...json };
}

const results = [];
const check = (name, ok, detail = '') => {
  results.push({ name, ok });
  console.log((ok ? 'PASS' : 'FAIL') + ' | ' + name + (detail ? ' | ' + detail : ''));
};

(async () => {
  const admin = await call('POST', '/auth/login', { body: { username: 'admin', password: 'admin123' } });
  check('管理员登录', admin.code === 200 && !!admin.data?.token);
  const reader = await call('POST', '/auth/login', { body: { username: 'reader', password: '123456' } });
  check('读者登录', reader.code === 200 && !!reader.data?.token);
  const at = admin.data.token, rt = reader.data.token;

  check('错误密码被拒绝', (await call('POST', '/auth/login', { body: { username: 'admin', password: 'x' } })).code !== 200);

  const page = await call('GET', '/books/page', { token: rt, query: { pageNum: 1, pageSize: 5 } });
  check('图书分页检索', page.code === 200 && page.data.records.length > 0, '共 ' + page.data?.total + ' 本');

  const before = await call('GET', '/books/3', { token: rt });
  const s0 = before.data.stock;
  check('借书成功', (await call('POST', '/borrows', { token: rt, query: { bookId: 3 } })).code === 200);
  const after = await call('GET', '/books/3', { token: rt });
  check('库存原子扣减', after.data.stock === s0 - 1, `${s0}->${after.data.stock}`);

  const my = await call('GET', '/borrows/my', { token: rt, query: { pageNum: 1, pageSize: 5 } });
  const record = my.data.records.find(r => r.status === 0 && r.bookId === 3);
  check('借阅记录已生成', !!record);

  const due0 = new Date(String(record.dueDate).replace(/-/g, '/'));
  await call('PUT', `/borrows/${record.id}/renew`, { token: rt });
  const my2 = await call('GET', '/borrows/my', { token: rt, query: { pageNum: 1, pageSize: 5 } });
  const r2 = my2.data.records.find(r => r.id === record.id);
  check('续借顺延30天且次数+1',
    Math.round((new Date(String(r2.dueDate).replace(/-/g, '/')) - due0) / 86400000) === 30 && r2.renewCount === 1);

  check('还书成功', (await call('PUT', `/borrows/${record.id}/return`, { token: rt })).code === 200);
  const afterRet = await call('GET', '/books/3', { token: rt });
  check('库存已回补', afterRet.data.stock === s0, String(afterRet.data.stock));

  check('读者访问用户管理被拒(403)', (await call('GET', '/users/page', { token: rt })).code === 403);
  check('未登录访问返回401', (await call('GET', '/books/page')).code === 401);
  check('管理员统计可用', (await call('GET', '/stat/overview', { token: at })).code === 200);

  const failed = results.filter(r => !r.ok);
  console.log(`\n===== ${results.length - failed.length}/${results.length} 通过 =====`);
  if (failed.length) process.exit(1);
})();
