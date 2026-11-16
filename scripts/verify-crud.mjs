const BASE = process.env.API_BASE || 'http://localhost:8080/api'

const results = []
function check(name, ok, extra = '') {
  results.push({ name, ok, extra })
  console.log(`${ok ? 'PASS' : 'FAIL'} | ${name}${extra ? ' | ' + extra : ''}`)
}

async function req(method, path, { token, body } = {}) {
  const res = await fetch(BASE + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: 'Bearer ' + token } : {})
    },
    body: body ? JSON.stringify(body) : undefined
  })
  return { status: res.status, json: await res.json().catch(() => ({})) }
}

const login = await req('POST', '/auth/login', {
  body: { username: 'admin', password: 'admin123' }
})
const token = login.json.data.token

// 1. 新增图书（DTO 校验通过）
const isbn = 'T' + Date.now()
const created = await req('POST', '/books', {
  token,
  body: { isbn, name: '包结构回归测试书', author: 'tester', stock: 3, price: 12.5 }
})
check('新增图书（BookDTO）', created.json.code === 200, created.json.message)

// 1.1 超长 ISBN 应在参数校验阶段被拦截（400），而不是落到数据库变 500
const tooLong = await req('POST', '/books', {
  token,
  body: { isbn: 'X'.repeat(30), name: '超长 ISBN', author: 'tester', stock: 1 }
})
check('超长 ISBN 返回 400 而非 500', tooLong.status === 400 && tooLong.json.code === 400, tooLong.json.message)

// 2. 检索到刚建的书，且默认上架、borrowed=0
const found = await req('GET', `/books/page?keyword=${encodeURIComponent(isbn)}`, { token })
const book = found.json.data?.records?.[0]
check('按 ISBN 检索到新书', !!book, book ? `${book.name} / 库存 ${book.stock} / 状态 ${book.status}` : '')
check('新增时 borrowed 由服务端置 0、status 默认上架', book?.borrowed === 0 && book?.status === 1)

// 3. 编辑时试图篡改库存应被忽略
const upd = await req('PUT', '/books', {
  token,
  body: { id: book.id, isbn, name: book.name, author: 'tester', stock: 999, price: 12.5 }
})
const after = await req('GET', `/books/${book.id}`, { token })
check('编辑图书成功', upd.json.code === 200, upd.json.message)
check('编辑接口忽略前端传入的库存（仍为 3）', after.json.data?.stock === 3, '实际 ' + after.json.data?.stock)

// 4. ISBN 重复应被业务异常拦截
const dup = await req('POST', '/books', {
  token,
  body: { isbn, name: '重复 ISBN 的书', author: 'tester', stock: 1 }
})
check('ISBN 重复被拒（400）', dup.status === 400 && dup.json.message === 'ISBN 已存在，请勿重复录入', dup.json.message)

// 5. 上下架
const off = await req('PUT', `/books/${book.id}/status?status=0`, { token })
const offBook = await req('GET', `/books/${book.id}`, { token })
check('下架图书', off.json.code === 200 && offBook.json.data?.status === 0)

// 6. 非法状态值应被拒
const badStatus = await req('PUT', `/books/${book.id}/status?status=5`, { token })
check('非法状态值被拒（400）', badStatus.status === 400, badStatus.json.message)

// 7. 分类：新建 + 重名拦截 + 编辑 + 删除
const catName = '回归测试分类' + Date.now()
const cat = await req('POST', '/categories', { token, body: { name: catName, description: '临时' } })
check('新增分类（CategoryDTO）', cat.json.code === 200, cat.json.message)

const catDup = await req('POST', '/categories', { token, body: { name: catName } })
check('分类重名被拒（400）', catDup.status === 400 && catDup.json.message === '分类名称已存在', catDup.json.message)

const catList = await req('GET', '/categories', { token })
const catRow = (catList.json.data || []).find(c => c.name === catName)
check('分类列表包含新分类', !!catRow)

// 8. 公告：新建 + 编辑
const notice = await req('POST', '/notices', {
  token,
  body: { title: '回归测试公告', content: '包结构调整后的写接口验证' }
})
check('发布公告（NoticeDTO）', notice.json.code === 200, notice.json.message)
const noticePage = await req('GET', '/notices/page?pageNum=1&pageSize=5', { token })
const noticeRow = (noticePage.json.data?.records || [])[0]
check('公告已落库并可查询', noticeRow?.title === '回归测试公告')
const noticeUpd = await req('PUT', `/notices/${noticeRow.id}`, {
  token,
  body: { title: '回归测试公告（已编辑）', content: '内容更新' }
})
check('编辑公告', noticeUpd.json.code === 200, noticeUpd.json.message)

// 9. 公告标题为空应被 @Valid 拦截
const noticeBad = await req('POST', '/notices', { token, body: { title: '', content: 'x' } })
check('公告标题为空被校验拦截（400）', noticeBad.status === 400 && noticeBad.json.code === 400, noticeBad.json.message)

// 清理测试数据
await req('DELETE', `/books/${book.id}`, { token })
if (catRow) await req('DELETE', `/categories/${catRow.id}`, { token })
const delNotice = await req('DELETE', `/notices/${noticeRow.id}`, { token })
check('测试数据已清理', delNotice.json.code === 200)

const passed = results.filter(r => r.ok).length
console.log(`\n===== ${passed}/${results.length} 通过 =====`)
process.exit(passed === results.length ? 0 : 1)
