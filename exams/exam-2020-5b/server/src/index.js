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

const names = ['Resident Evil 2',
  'Kingdom Hearts 3',
  'Anthem',
  'Crackdown 3',
  'Metro: Exodus',
  'Devil May Cry 5',
  'The Division 2',
  'Sekiro: Shadows Die Twice',
  'OUTER WILDS',
  'CONTROL'];
const statuses = ['available', 'missing', 'borrowed', 'canceled'];
const users = ['John', 'Joe', 'Jim', 'Jone', 'Joan'];
const games = [];

for (let i = 0; i < 50; i++) {
  games.push({
    id: i + 1,
    name: names[getRandomInt(0, names.length)],
    status: statuses[getRandomInt(0, statuses.length)],
    user: users[getRandomInt(0, users.length)],
    size: getRandomInt(2000, 50000),
    popularityScore: getRandomInt(0, 100)
  });
}

const router = new Router();

router.get('/allGames', ctx => {
  ctx.response.body = games;
  ctx.response.status = 200;
});

router.get('/ready', ctx => {
  ctx.response.body = games.filter(game => game.status === 'available');
  ctx.response.status = 200;
});


router.get('/games/:user', ctx => {
  const headers = ctx.params;
  const user = headers.user;
  if (typeof user !== 'undefined') {
    ctx.response.body = games.filter(game => game.user == user);
    ctx.response.status = 200;
  } else {
    console.log("Missing or invalid: user!");
    ctx.response.body = {text: 'Missing or invalid: user!'};
    ctx.response.status = 404;
  }
});

router.post('/book', ctx => {
  // console.log("ctx: " + JSON.stringify(ctx));
  const headers = ctx.request.body;
  // console.log("body: " + JSON.stringify(headers));
  const id = headers.id;
  const user = headers.user;
  if (typeof id !== 'undefined' && typeof user !== 'undefined') {
    const index = games.findIndex(game => game.id == id);
    if (index === -1) {
      console.log("Game not available!");
      ctx.response.body = {text: 'Game not available!'};
      ctx.response.status = 404;
    } else {
      let game = games[index];
      game.user = user;
      game.status = 'borrowed';
      // console.log("user changed: " + JSON.stringify(game));
      ctx.response.body = game;
      ctx.response.status = 200;
    }
  } else {
    console.log("Missing or invalid: id or user!");
    ctx.response.body = {text: 'Missing or invalid: id or user!'};
    ctx.response.status = 404;
  }
});

const broadcast = (data) =>
  wss.clients.forEach((client) => {
    if (client.readyState === WebSocket.OPEN) {
      client.send(JSON.stringify(data));
    }
  });

router.post('/game', ctx => {
  // console.log("ctx: " + JSON.stringify(ctx));
  const headers = ctx.request.body;
  // console.log("body: " + JSON.stringify(headers));
  const name = headers.name;
  const user = headers.user;
  const size = headers.size;
  const popularityScore = headers.popularityScore;
  if (typeof name !== 'undefined' && typeof user !== 'undefined'
    && typeof size !== 'undefined' && popularityScore !== 'undefined') {
    const index = games.findIndex(game => game.name == name && game.user == user);
    if (index !== -1) {
      console.log("Game already exists!");
      ctx.response.body = {text: 'Game already exists!'};
      ctx.response.status = 404;
    } else {
      let maxId = Math.max.apply(Math, games.map(function (game) {
        return game.id;
      })) + 1;
      let obj = {
        id: maxId,
        name,
        status: 'available',
        user,
        size,
        popularityScore
      };
      // console.log("created: " + JSON.stringify(name));
      games.push(obj);
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

server.listen(2502);