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

const names = ['Charlize',
  'JoeBlack',
  'Joe',
  'Steph',
  'Pirate',
  'Captain',
  'TheVile',
  'Sinisterbrand',
  'Admiral',
  'Stephplank'];
const statuses = ['shared', 'open', 'draft', 'secret'];
const locations = ['Sudoldo', 'Landca', 'Rekitts', 'Rusqasuco', 'Momaar', 'Jack'];
const files = [];

for (let i = 0; i < 100; i++) {
  files.push({
    id: i + 1,
    name: names[getRandomInt(0, names.length)],
    status: statuses[getRandomInt(0, statuses.length)],
    location: locations[getRandomInt(0, locations.length)],
    size: getRandomInt(200, 2000),
    usage: getRandomInt(0, 10)
  });
}

const router = new Router();

router.get('/all', ctx => {
  ctx.response.body = files;
  ctx.response.status = 200;
});

router.get('/locations', ctx => {
  ctx.response.body = [...new Set(files.map(obj => obj.location))];
  ctx.response.status = 200;
});


router.get('/files/:location', ctx => {
  const headers = ctx.params;
  const location = headers.location;
  if (typeof location !== 'undefined') {
    ctx.response.body = files.filter(obj => obj.location == location);
    ctx.response.status = 200;
  } else {
    console.log("Missing or invalid: location!");
    ctx.response.body = {text: 'Missing or invalid: location!'};
    ctx.response.status = 404;
  }
});

const broadcast = (data) =>
  wss.clients.forEach((client) => {
    if (client.readyState === WebSocket.OPEN) {
      client.send(JSON.stringify(data));
    }
  });

router.post('/file', ctx => {
  // console.log("ctx: " + JSON.stringify(ctx));
  const headers = ctx.request.body;
  // console.log("body: " + JSON.stringify(headers));
  const name = headers.name;
  const location = headers.location;
  const size = headers.size;
  const used = headers.used;
  if (typeof name !== 'undefined' && typeof location !== 'undefined'
    && typeof size !== 'undefined' && used !== 'undefined') {
    const index = files.findIndex(obj => obj.name == name && obj.location == location);
    if (index !== -1) {
      console.log("File already exists!");
      ctx.response.body = {text: 'File already exists!'};
      ctx.response.status = 404;
    } else {
      let maxId = Math.max.apply(Math, files.map(function (obj) {
        return obj.id;
      })) + 1;
      let obj = {
        id: maxId,
        name,
        status: 'open',
        location,
        size,
        used
      };
      // console.log("created: " + JSON.stringify(obj));
      files.push(obj);
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

router.del('/file/:id', ctx => {
  // console.log("ctx: " + JSON.stringify(ctx));
  const headers = ctx.params;
  // console.log("body: " + JSON.stringify(headers));
  const id = headers.id;
  if (typeof id !== 'undefined') {
    const index = files.findIndex(obj => obj.id == id);
    if (index === -1) {
      console.log("No file with id: " + id);
      ctx.response.body = {text: 'Invalid file id'};
      ctx.response.status = 404;
    } else {
      let obj = files[index];
      // console.log("deleting: " + JSON.stringify(obj));
      files.splice(index, 1);
      ctx.response.body = obj;
      ctx.response.status = 200;
    }
  } else {
    console.log("Missing or invalid fields!");
    ctx.response.body = {text: 'Id missing or invalid'};
    ctx.response.status = 404;
  }
});

app.use(router.routes());
app.use(router.allowedMethods());

server.listen(2702);