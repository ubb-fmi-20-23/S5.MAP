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

const modelNames = ['M1', 'M2', 'Huge', 'Big', 'Small', 'Round'];
const statuses = ['ordered', 'printing', 'done', 'failed', 'canceled'];
const models = [];

for (let i = 0; i < 50; i++) {
  models.push({
    id: i + 1,
    model: modelNames[getRandomInt(0, modelNames.length)],
    status: statuses[getRandomInt(0, statuses.length)],
    client: getRandomInt(1, 5),
    time: getRandomInt(10, 200),
    cost: getRandomInt(10, 500)
  });
}

const router = new Router();

router.get('/all', ctx => {
  ctx.response.body = models;
  ctx.response.status = 200;
});

router.get('/models/:clientId', ctx => {
  const headers = ctx.params;
  const clientId = headers.clientId;
  if (typeof clientId !== 'undefined') {
    ctx.response.body = models.filter(model => model.client == clientId);
    ctx.response.status = 200;
  } else {
    console.log("Missing or invalid: clientId!");
    ctx.response.body = {text: 'Missing or invalid: clientId!'};
    ctx.response.status = 404;
  }
});

router.post('/process', ctx => {
  // console.log("ctx: " + JSON.stringify(ctx));
  const headers = ctx.request.body;
  // console.log("body: " + JSON.stringify(headers));
  const id = headers.id;
  const status = headers.status;
  if (typeof id !== 'undefined' && typeof status !== 'undefined') {
    const index = models.findIndex(model => model.id == id);
    if (index === -1) {
      console.log("Model not available!");
      ctx.response.body = {text: 'Model not available!'};
      ctx.response.status = 404;
    } else {
      let model = models[index];
      model.status = status;
      // console.log("status changed: " + JSON.stringify(model));
      ctx.response.body = model;
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

router.post('/model', ctx => {
  // console.log("ctx: " + JSON.stringify(ctx));
  const headers = ctx.request.body;
  // console.log("body: " + JSON.stringify(headers));
  const model = headers.model;
  const status = headers.status;
  const client = headers.client;
  const time = headers.time;
  const cost = headers.cost;
  if (typeof model !== 'undefined' && typeof client !== 'undefined' && typeof status !== 'undefined'
    && typeof time !== 'undefined' && cost !== 'undefined') {
    const index = models.findIndex(model => model.model == model && model.client == client);
    if (index !== -1) {
      console.log("Model already exists!");
      ctx.response.body = {text: 'Model already exists!'};
      ctx.response.status = 404;
    } else {
      let maxId = Math.max.apply(Math, models.map(function (model) {
        return model.id;
      })) + 1;
      let obj = {
        id: maxId,
        model,
        status,
        client,
        time,
        cost
      };
      // console.log("created: " + JSON.stringify(model));
      models.push(obj);
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

server.listen(2901);