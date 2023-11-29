const koa = require('koa');
const app = new koa();
const server = require('http').createServer(app.callback());
const WebSocket = require('ws');
const wss = new WebSocket.Server({server});
const Router = require('koa-router');
const cors = require('koa-cors');
const bodyParser = require('koa-bodyparser');
const convert = require('koa-convert');

app.use(bodyParser());
app.use(convert(cors()));
app.use(async (ctx, next) => {
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

const bookmarkNames = ['The good', 'The bad', 'The ugly', 'Old', 'New', 'Blue'];
const descriptionNames = ['a thing', 'another thing', 'not useful', 'very useful', 'broken'];
const sampleUrls = ['google.com', 'yahoo.com', 'ubbcluj.ro', 'youtube.com'];
const types = ['text', 'video', 'link', 'note'];
const bookmarks = [];
for (let i = 0; i < 10; i++) {
    bookmarks.push({
        id: i + 1,
        name: bookmarkNames[getRandomInt(0, bookmarkNames.length)],
        description: descriptionNames[getRandomInt(0, descriptionNames.length)],
        url: sampleUrls[getRandomInt(0, sampleUrls.length)],
        type: types[getRandomInt(0, types.length)],
        rating: getRandomInt(0, 100)
    });
}

const router = new Router();
router.get('/types', ctx => {
    ctx.response.body = types;
    ctx.response.status = 200;
});

router.get('/bookmarks/:type', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.params;
    const type = headers.type;
    // console.log("genre: " + JSON.stringify(genre));
    ctx.response.body = bookmarks.filter(bookmark => bookmark.type == type);
    // console.log("body: " + JSON.stringify(ctx.response.body));
    ctx.response.status = 200;
});

router.get('/underrated', ctx => {
    ctx.response.body = bookmarks;
    ctx.response.status = 200;
});

router.post('/rate', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.request.body;
    // console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    if (typeof id !== 'undefined') {
        const index = bookmarks.findIndex(bookmark => bookmark.id == id);
        if (index === -1) {
            console.log("Bookmark not available!");
            ctx.response.body = {text: 'Bookmark not available!'};
            ctx.response.status = 404;
        } else {
            let bookmark = bookmarks[index];
            bookmark.rating++;
            ctx.response.body = bookmark;
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

router.post('/bookmark', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.request.body;
    // console.log("body: " + JSON.stringify(headers));
    const name = headers.name;
    const description = headers.description;
    const url = headers.url;
    const type = headers.type;
    if (typeof name !== 'undefined' && typeof description !== 'undefined' && typeof url !== 'undefined' && typeof type !== 'undefined') {
        const index = bookmarks.findIndex(bookmark => bookmark.name == name);
        if (index !== -1) {
            console.log("Bookmark already exists!");
            ctx.response.body = {text: 'Bookmark already exists!'};
            ctx.response.status = 404;
        } else {
            let maxId = Math.max.apply(Math, bookmarks.map(function (bookmark) {
                return bookmark.id;
            })) + 1;
            let bookmark = {
                id: maxId,
                name,
                description,
                url,
                type,
                rating: 1
            };
            bookmarks.push(bookmark);
            broadcast(bookmark);
            ctx.response.body = bookmark;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: name, price, quantity or type!");
        ctx.response.body = {text: 'Missing or invalid: name, price, quantity or type!'};
        ctx.response.status = 404;
    }
});

router.del('/bookmark/:id', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.params;
    // console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    if (typeof id !== 'undefined') {
        const index = bookmarks.findIndex(bookmark => bookmark.id == id);
        if (index === -1) {
            console.log("No bookmark with id: " + id);
            ctx.response.body = {text: 'Invalid bookmark id'};
            ctx.response.status = 404;
        } else {
            let bookmark = bookmarks[index];
            bookmarks.splice(index, 1);
            ctx.response.body = bookmark;
            ctx.response.status = 200;
        }
    } else {
        ctx.response.body = {text: 'Id missing or invalid'};
        ctx.response.status = 404;
    }
});

app.use(router.routes());
app.use(router.allowedMethods());

server.listen(2230);
