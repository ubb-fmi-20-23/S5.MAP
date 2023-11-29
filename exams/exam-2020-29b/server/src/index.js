var koa = require('koa');
var app = module.exports = new koa();
const server = require('http').createServer(app.callback());
const WebSocket = require('ws');
const wss = new WebSocket.Server({server});
const Router = require('koa-router');
const cors = require('@koa/cors');
const bodyParser = require('koa-bodyparser');

app.use(bodyParser());

app.use(cors());

app.use(middleware);

function middleware(ctx, next) {
  const start = new Date();
  return next().then(() => {
    const ms = new Date() - start;
    console.log(`${start.toLocaleTimeString()} ${ctx.request.method} ${ctx.request.url} ${ctx.response.status} - ${ms}ms`);
  });
}

const getRandomInt = (min, max) => {
  min = Math.ceil(min);
  max = Math.floor(max);
  return Math.floor(Math.random() * (max - min)) + min;
};

const expenseNames = ['E1', 'E2', 'Huge', 'Big', 'Small', 'Something'];
const statuses = ['open', 'filled', 'postponed', 'canceled'];
const studentNames = ['John', 'Joe', 'Jane', 'Jude', 'Jack', 'June'];
const requests = [];

for (let i = 0; i < 50; i++) {
  requests.push({
    id: i + 1,
    name: expenseNames[getRandomInt(0, expenseNames.length)],
    status: statuses[getRandomInt(0, statuses.length)],
    student: studentNames[getRandomInt(0, studentNames.length)],
    eCost: getRandomInt(10, 200),
    cost: getRandomInt(1, 500)
  });
}

const router = new Router();

router.get('/open', ctx => {
  ctx.response.body = requests.filter(request => request.status === 'open');
  ;
  ctx.response.status = 200;
});


router.get('/filled', ctx => {
  ctx.response.body = requests.filter(request => request.status === 'filled');
  ctx.response.status = 200;
});

router.get('/my/:student', ctx => {
  const headers = ctx.params;
  const student = headers.student;
  if (typeof student !== 'undefined') {
    ctx.response.body = requests.filter(request => request.student == student);
    ctx.response.status = 200;
  } else {
    console.log("Missing or invalid: clientId!");
    ctx.response.body = {text: 'Missing or invalid: clientId!'};
    ctx.response.status = 404;
  }
});

router.post('/change', ctx => {
  // console.log("ctx: " + JSON.stringify(ctx));
  const headers = ctx.request.body;
  // console.log("body: " + JSON.stringify(headers));
  const id = headers.id;
  const status = headers.status;
  const cost = headers.cost;
  if (typeof id !== 'undefined' && typeof status !== 'undefined') {
    const index = requests.findIndex(request => request.id == id);
    if (index === -1) {
      console.log("Model not available!");
      ctx.response.body = {text: 'Model not available!'};
      ctx.response.status = 404;
    } else {
      let request = requests[index];
      request.status = status;
      if (status === 'filled') {
        request.cost = cost;
      }
      // console.log("status changed: " + JSON.stringify(request));
      ctx.response.body = request;
      ctx.response.status = 200;
    }
  } else {
    console.log("Missing or invalid: id!");
    ctx.response.body = {text: 'Missing or invalid: id!'};
    ctx.response.status = 404;
  }
});

const broadcast = (data) =>
  wss.clients.forEach((client) => {
    if (client.readyState === WebSocket.OPEN) {
      client.send(JSON.stringify(data));
    }
  });

router.post('/request', ctx => {
  // console.log("ctx: " + JSON.stringify(ctx));
  const headers = ctx.request.body;
  // console.log("body: " + JSON.stringify(headers));
  const name = headers.name;
  const status = headers.status;
  const student = headers.student;
  const eCost = headers.eCost;
  const cost = headers.cost;
  if (typeof name !== 'undefined' && typeof student !== 'undefined' && typeof status !== 'undefined'
    && typeof eCost !== 'undefined' && cost !== 'undefined') {
    const index = requests.findIndex(request => request.name == name && request.student == student);
    if (index !== -1) {
      console.log("Request already exists!");
      ctx.response.body = {text: 'Request already exists!'};
      ctx.response.status = 404;
    } else {
      let maxId = Math.max.apply(Math, requests.map(function (request) {
        return request.id;
      })) + 1;
      let obj = {
        id: maxId,
        name,
        status,
        student,
        eCost,
        cost
      };
      // console.log("created: " + JSON.stringify(request));
      requests.push(obj);
      broadcast(obj);
      ctx.response.body = obj;
      ctx.response.status = 200;
    }
  } else {
    console.log("Missing or invalid fields!");
    ctx.response.body = {text: 'Missing or invalid fields!'};
    ctx.response.status = 404;
  }
});


app.use(router.routes());
app.use(router.allowedMethods());

server.listen(2902);