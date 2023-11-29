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

const tableTypes = ['small', 'big', 'private'];
const statusTypes = ['free', 'used'];
const tables = [];
for (let i = 0; i < 10; i++) {
    tables.push({
        id: i + 1,
        name: "Table " + (i + 1),
        type: tableTypes[getRandomInt(0, tableTypes.length - 1)],
        status: statusTypes[0],
        seats: (i + 1) * 2
    });
}

const router = new Router();
router.get('/tables', ctx => {
    ctx.response.body = tables;
    ctx.response.status = 200;
});

router.get('/free', ctx => {
    ctx.response.body = tables.filter(room => room.status === 'free');
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
        const index = tables.findIndex(table => table.name === name && table.type === type);
        if (index === -1) {
            let maxId = Math.max.apply(Math, tables.map(function (table) {
                    return table.id;
                })) + 1;
            let table = {
                id: maxId,
                name,
                type,
                status: statusTypes[0],
                size
            };
            tables.push(table);
            broadcast(table);
            ctx.response.body = table;
            ctx.response.status = 200;
        } else {
            ctx.response.body = {text: 'The table already exists'};
            ctx.response.status = 404;
        }
    } else {
        ctx.response.body = {text: 'Missing name, type and size fields'};
        ctx.response.status = 404;
    }
});

router.post('/del', ctx => {
    const headers = ctx.request.body;
    console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    if (typeof id != 'undefined') {
        const index = tables.findIndex(table => table.id === id);
        if (index === -1) {
            ctx.response.body = {text: 'Invalid id'};
            ctx.response.status = 404;
        } else {
            tables.splice(index, 1);
            ctx.response.body = {text: 'The table was deleted'};
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
        const index = tables.findIndex(table => table.id === id);
        if (index === -1) {
            ctx.response.body = {text: 'The table is missing'};
            ctx.response.status = 404;
        } else {
            let table = tables[index];
            table.name = name;
            table.type = type;
            table.size = size;
            ctx.response.body = table;
            ctx.response.status = 200;
        }
    } else {
        ctx.response.body = {text: 'Missing id, name, type or size field'};
        ctx.response.status = 404;
    }
});


router.post('/reserve', ctx => {
    const headers = ctx.request.body;
    console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    if (typeof id != 'undefined') {
        const index = tables.findIndex(table => table.id === id);
        if (index === -1) {
            ctx.response.body = {text: 'Invalid id'};
            ctx.response.status = 404;
        } else {
            let table = tables[index];
            if (table.status != statusTypes[0]) {
                ctx.response.body = {text: 'The table is already taken'};
                ctx.response.status = 404;
            } else {
                table.status = statusTypes[1];
                ctx.response.body = table;
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
        const index = tables.findIndex(table => table.id === id);
        if (index === -1) {
            ctx.response.body = {text: 'Invalid id'};
            ctx.response.status = 404;
        } else {
            let room = tables[index];
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

server.listen(3100);