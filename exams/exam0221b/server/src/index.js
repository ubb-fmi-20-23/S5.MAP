const Koa = require('koa');
const app = new Koa();
const server = require('http').createServer(app.callback());
const WebSocket = require('ws');
const wss = new WebSocket.Server({server});
const Router = require('koa-router');
const cors = require('koa-cors');
const bodyparser = require('koa-bodyparser');

app.use(bodyparser());
app.use(cors());
app.use(async function (ctx, next) {
    const start = new Date();
    await next();
    const ms = new Date() - start;
    console.log(`${ctx.method} ${ctx.url} ${ctx.response.status} - ${ms}ms`);
});

const getRandomInt = (min, max) => {
    min = Math.ceil(min);
    max = Math.floor(max);
    return Math.floor(Math.random() * (max - min)) + min;
};

const gameTypes = ['action', 'adventure', 'puzzle', 'shooter'];
const statusTypes = ['available', 'taken'];
const games = [];
for (let i = 0; i < 10; i++) {
    games.push({
        id: i + 1,
        name: "Game " + (i + 1),
        type: gameTypes[getRandomInt(0, gameTypes.length - 1)],
        status: statusTypes[0],
        size: i * 2
    });
}

const router = new Router();
router.get('/games', ctx => {
    ctx.response.body = games;
    ctx.response.status = 200;
});

router.get('/unused', ctx => {
    ctx.response.body = games.filter(room => room.status === 'available');
    ctx.response.status = 200;
});


const broadcast = (data) =>
    wss.clients.forEach((client) => {
        if (client.readyState === WebSocket.OPEN) {
            client.send(JSON.stringify(data));
        }
    });

router.post('/add', ctx => {
    const headers = ctx.request.body;
    console.log("body: " + JSON.stringify(headers));
    const name = headers.name;
    const type = headers.type;
    const size = headers.size;
    if (typeof name != 'undefined' && typeof type != 'undefined' && typeof size != 'undefined') {
        const index = games.findIndex(game => game.name === name && game.type === type);
        if (index === -1) {
            let maxId = Math.max.apply(Math, games.map(function (game) {
                    return game.id;
                })) + 1;
            let game = {
                id: maxId,
                name,
                type,
                status: statusTypes[0],
                size
            };
            games.push(game);
            broadcast(game);
            ctx.response.body = game;
            ctx.response.status = 200;
        } else {
            ctx.response.body = {text: 'The game already exists'};
            ctx.response.status = 404;
        }
    } else {
        ctx.response.body = {text: 'Missing name, type and size fields'};
        ctx.response.status = 404;
    }
});

router.post('/remove', ctx => {
    const headers = ctx.request.body;
    console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    if (typeof id != 'undefined') {
        const index = games.findIndex(game => game.id === id);
        if (index === -1) {
            ctx.response.body = {text: 'Invalid id'};
            ctx.response.status = 404;
        } else {
            games.splice(index, 1);
            ctx.response.body = {text: 'The game was deleted'};
            ctx.response.status = 200;
        }
    } else {
        ctx.response.body = {text: 'Id missing'};
        ctx.response.status = 404;
    }
});

router.post('/update', ctx => {
    const headers = ctx.request.body;
    console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    const name = headers.name;
    const type = headers.type;
    const size = headers.size;
    if (typeof id != 'undefined' && typeof name != 'undefined' && typeof type != 'undefined' && typeof size != 'undefined') {
        const index = games.findIndex(game => game.id === id);
        if (index === -1) {
            ctx.response.body = {text: 'The game is missing'};
            ctx.response.status = 404;
        } else {
            let game = games[index];
            game.name = name;
            game.type = type;
            game.size = size;
            ctx.response.body = game;
            ctx.response.status = 200;
        }
    } else {
        ctx.response.body = {text: 'Missing id, name, type or size field'};
        ctx.response.status = 404;
    }
});


router.post('/take', ctx => {
    const headers = ctx.request.body;
    console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    if (typeof id != 'undefined') {
        const index = games.findIndex(game => game.id === id);
        if (index === -1) {
            ctx.response.body = {text: 'Invalid id'};
            ctx.response.status = 404;
        } else {
            let game = games[index];
            if (game.status != statusTypes[0]) {
                ctx.response.body = {text: 'The game is already taken'};
                ctx.response.status = 404;
            } else {
                game.status = statusTypes[1];
                ctx.response.body = game;
                ctx.response.status = 200;
            }
        }
    } else {
        ctx.response.body = {text: 'Id missing'};
        ctx.response.status = 404;
    }
});

router.post('/release', ctx => {
    const headers = ctx.request.body;
    console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    if (typeof id != 'undefined') {
        const index = games.findIndex(game => game.id === id);
        if (index === -1) {
            ctx.response.body = {text: 'Invalid id'};
            ctx.response.status = 404;
        } else {
            let room = games[index];
            room.status = statusTypes[0];
            ctx.response.body = room;
            ctx.response.status = 200;
        }
    } else {
        ctx.response.body = {text: 'Id missing'};
        ctx.response.status = 404;
    }
});


app.use(router.routes());
app.use(router.allowedMethods());

server.listen(4000);