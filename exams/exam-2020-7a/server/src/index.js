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

const names = ['Charlize Blondebeard',
  'Sinister Joe Black',
  'Vile Joe',
  'Beautiful Steph',
  'Dread Pirate Charlize', '' +
  'Captain Joe',
  'The Vile Pirate',
  'Charlize Sinisterbrand',
  'Admiral Joe',
  'Stephplank'];
const statuses = ['shared', 'open', 'draft', 'secret'];
const owners = ['John', 'Joe', 'Jim', 'Jone', 'Joan', 'Jack'];
const documents = [];

for (let i = 0; i < 50; i++) {
  documents.push({
    id: i + 1,
    name: names[getRandomInt(0, names.length)],
    status: statuses[getRandomInt(0, statuses.length)],
    owner: owners[getRandomInt(0, owners.length)],
    size: getRandomInt(200, 2000),
    usage: getRandomInt(0, 10)
  });
}

const router = new Router();

router.get('/all', ctx => {
  ctx.response.body = documents;
  ctx.response.status = 200;
});

router.get('/documents/:owner', ctx => {
  const headers = ctx.params;
  const owner = headers.owner;
  if (typeof owner !== 'undefined') {
    ctx.response.body = documents.filter(obj => obj.owner == owner);
    ctx.response.status = 200;
  } else {
    console.log("Missing or invalid: owner!");
    ctx.response.body = {text: 'Missing or invalid: owner!'};
    ctx.response.status = 404;
  }
});

const broadcast = (data) =>
  wss.clients.forEach((client) => {
    if (client.readyState === WebSocket.OPEN) {
      client.send(JSON.stringify(data));
    }
  });

router.post('/document', ctx => {
  // console.log("ctx: " + JSON.stringify(ctx));
  const headers = ctx.request.body;
  // console.log("body: " + JSON.stringify(headers));
  const name = headers.name;
  const owner = headers.owner;
  const size = headers.size;
  const used = headers.used;
  if (typeof name !== 'undefined' && typeof owner !== 'undefined'
    && typeof size !== 'undefined' && used !== 'undefined') {
    const index = documents.findIndex(obj => obj.name == name && obj.owner == owner);
    if (index !== -1) {
      console.log("Document already exists!");
      ctx.response.body = {text: 'Document already exists!'};
      ctx.response.status = 404;
    } else {
      let maxId = Math.max.apply(Math, documents.map(function (obj) {
        return obj.id;
      })) + 1;
      let obj = {
        id: maxId,
        name,
        status: 'open',
        owner,
        size,
        used
      };
      // console.log("created: " + JSON.stringify(name));
      documents.push(obj);
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

router.del('/document/:id', ctx => {
  // console.log("ctx: " + JSON.stringify(ctx));
  const headers = ctx.params;
  // console.log("body: " + JSON.stringify(headers));
  const id = headers.id;
  if (typeof id !== 'undefined') {
    const index = documents.findIndex(obj => obj.id == id);
    if (index === -1) {
      console.log("No document with id: " + id);
      ctx.response.body = {text: 'Invalid document id'};
      ctx.response.status = 404;
    } else {
      let obj = documents[index];
      // console.log("deleting: " + JSON.stringify(obj));
      documents.splice(index, 1);
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

server.listen(2701);