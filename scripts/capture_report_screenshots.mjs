import fs from 'node:fs/promises'
import path from 'node:path'

const port = Number(process.argv[2] || 9333)
const outputDir = path.resolve(process.argv[3] || 'reports/screenshots')
const username = process.env.REPORT_DEMO_USERNAME
const password = process.env.REPORT_DEMO_PASSWORD

if (!username || !password) {
  throw new Error('请通过 REPORT_DEMO_USERNAME 和 REPORT_DEMO_PASSWORD 提供临时演示账号')
}

await fs.mkdir(outputDir, { recursive: true })

const targets = await (await fetch(`http://127.0.0.1:${port}/json/list`)).json()
const target = targets.find((item) => item.type === 'page')
if (!target) throw new Error('没有找到可用的 Chrome 页面目标')

const socket = new WebSocket(target.webSocketDebuggerUrl)
await new Promise((resolve, reject) => {
  socket.addEventListener('open', resolve, { once: true })
  socket.addEventListener('error', reject, { once: true })
})

let nextId = 1
const pending = new Map()
socket.addEventListener('message', (event) => {
  const message = JSON.parse(event.data)
  if (!message.id) return
  const handler = pending.get(message.id)
  if (!handler) return
  pending.delete(message.id)
  if (message.error) handler.reject(new Error(message.error.message))
  else handler.resolve(message.result)
})

function send(method, params = {}) {
  const id = nextId++
  socket.send(JSON.stringify({ id, method, params }))
  return new Promise((resolve, reject) => pending.set(id, { resolve, reject }))
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

async function evaluate(expression) {
  const result = await send('Runtime.evaluate', {
    expression,
    awaitPromise: true,
    returnByValue: true,
  })
  if (result.exceptionDetails) throw new Error(result.exceptionDetails.text)
  return result.result.value
}

async function waitFor(expression, timeoutMs = 60000) {
  const started = Date.now()
  while (Date.now() - started < timeoutMs) {
    if (await evaluate(expression)) return
    await sleep(250)
  }
  throw new Error(`等待条件超时：${expression}`)
}

async function navigate(url, readyExpression) {
  await send('Page.navigate', { url })
  await waitFor(`document.readyState === 'complete' || document.readyState === 'interactive'`)
  if (readyExpression) await waitFor(readyExpression)
  await sleep(1200)
}

async function screenshot(name) {
  const result = await send('Page.captureScreenshot', {
    format: 'png',
    captureBeyondViewport: false,
    fromSurface: true,
  })
  await fs.writeFile(path.join(outputDir, name), Buffer.from(result.data, 'base64'))
}

await send('Page.enable')
await send('Runtime.enable')

await navigate('http://127.0.0.1:5173/login', `Boolean(document.querySelector('#login-username'))`)
await screenshot('01-login.png')

await evaluate(`(() => {
  const setValue = (selector, value) => {
    const input = document.querySelector(selector)
    const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set
    setter.call(input, value)
    input.dispatchEvent(new Event('input', { bubbles: true }))
  }
  setValue('#login-username', ${JSON.stringify(username)})
  setValue('#login-password', ${JSON.stringify(password)})
  const button = [...document.querySelectorAll('button')].find((item) => item.textContent.includes('登录'))
  button.click()
  return true
})()`)
await waitFor(`location.pathname === '/dashboard'`)
await sleep(1800)
await screenshot('02-dashboard.png')

const pages = [
  ['/entries', '03-entries.png', `Boolean(document.querySelector('.entries-view')) || document.body.innerText.includes('收支')`],
  ['/members', '04-members.png', `document.body.innerText.includes('家庭通行证') || document.body.innerText.includes('家庭成员')`],
  ['/categories', '05-categories.png', `document.body.innerText.includes('收支分类') || document.body.innerText.includes('分类')`],
  ['/profile', '06-profile.png', `document.body.innerText.includes('个人资料') || document.body.innerText.includes('修改密码')`],
]

for (const [route, name, ready] of pages) {
  await navigate(`http://127.0.0.1:5173${route}`, ready)
  await screenshot(name)
}

socket.close()
console.log(JSON.stringify({ outputDir, files: (await fs.readdir(outputDir)).sort() }, null, 2))
