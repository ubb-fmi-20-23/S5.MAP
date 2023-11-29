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

const names = ['Cersei Lannister', 'Margaery Tyrell', 'Tyrion Lannister', 'Ramsay Snow', 'Jon Snow', 'Daenerys Targaryen', 'Gared Tuttle', 'Mira Forrester'];
const descriptions = ['a thing', 'another thing', 'not useful', 'very useful', 'broken'];
const types = ['per year', 'per quarter', 'per month', 'per week', 'per day'];
const statuses = ['new', 'open', 'closed'];
const tuitions = [];
for (let i = 0; i < 20; i++) {
    tuitions.push({
        id: i + 1,
        name: names[getRandomInt(0, names.length)],
        description: descriptions[getRandomInt(0, descriptions.length)],
        type: types[getRandomInt(0, types.length)],
        due: getRandomInt(2019, 2200),
        status: statuses[getRandomInt(0, statuses.length)],
        amount: getRandomInt(10, 200)
    });
}

const router = new Router();

router.get('/tuitions', ctx => {
    ctx.response.body = tuitions;
    ctx.response.status = 200;
});

router.get('/genres', ctx => {
    ctx.response.body = genres;
    ctx.response.status = 200;
});

router.get('/myTuitions/:name', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.params;
    const name = headers.name;
    if (typeof name !== 'undefined') {
        ctx.response.body = tuitions.filter(tuition => tuition.name == name);
        ctx.response.status = 200;
    } else {
        console.log("Missing or invalid: name!");
        ctx.response.body = {text: 'Missing or invalid: name!'};
        ctx.response.status = 404;
    }
});

router.get('/tuition/:id', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.params;
    const id = headers.id;
    if (typeof id !== 'undefined') {
        const index = tuitions.findIndex(tuition => tuition.id == id);
        if (index === -1) {
            console.log("Tuition not available!");
            ctx.response.body = {text: 'Tuition not available!'};
            ctx.response.status = 404;
        } else {
            let tuition = tuitions[index];
            ctx.response.body = tuition;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: id!");
        ctx.response.body = {text: 'Missing or invalid: id!'};
        ctx.response.status = 404;
    }
});

router.post('/tuition', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.request.body;
    // console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    const name = headers.name;
    const description = headers.description;
    const type = headers.type;
    const due = headers.due;
    const status = headers.status;
    const amount = headers.amount;
    if (typeof id !== 'undefined' && typeof name !== 'undefined' && typeof description !== 'undefined'
        && typeof type !== 'undefined' && typeof due !== 'undefined' && typeof status !== 'undefined'
        && typeof amount !== 'undefined') {
        const index = tuitions.findIndex(tuition => tuition.id == id);
        if (index === -1) {
            console.log("Tuition not available!");
            ctx.response.body = {text: 'Tuition not available!'};
            ctx.response.status = 404;
        } else {
            let tuition = tuitions[index];
            tuition.name = name;
            tuition.description = description;
            tuition.type = type;
            tuition.due = due;
            tuition.status = status;
            tuition.amount = amount;
            ctx.response.body = tuition;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: id, name, description, type, due, status or amount!");
        ctx.response.body = {text: 'Missing or invalid: id, name, description, type, due, status or amount!'};
        ctx.response.status = 404;
    }
});

router.post('/updateStatus', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.request.body;
    // console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    const status = headers.status;
    if (typeof id !== 'undefined' && typeof status !== 'undefined') {
        const index = tuitions.findIndex(tuition => tuition.id == id);
        if (index === -1) {
            console.log("Tuition not available!");
            ctx.response.body = {text: 'Tuition not available!'};
            ctx.response.status = 404;
        } else {
            let tuition = tuitions[index];
            tuition.status = status;
            ctx.response.body = tuition;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: id or status!");
        ctx.response.body = {text: 'Missing or invalid: id or status!'};
        ctx.response.status = 404;
    }
});

router.post('/pay', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.request.body;
    // console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    const amount = headers.amount;
    if (typeof id !== 'undefined' && typeof amount !== 'undefined') {
        const index = tuitions.findIndex(tuition => tuition.id == id);
        if (index === -1) {
            console.log("Tuition not available!");
            ctx.response.body = {text: 'Tuition not available!'};
            ctx.response.status = 404;
        } else {
            let tuition = tuitions[index];
            tuition.amount -= amount;
            ctx.response.body = tuition;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: id or amount!");
        ctx.response.body = {text: 'Missing or invalid: id or amount!'};
        ctx.response.status = 404;
    }
});


router.del('/tuition/:id', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.params;
    // console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    if (typeof id !== 'undefined') {
        const index = tuitions.findIndex(tuition => tuition.id == id);
        if (index === -1) {
            console.log("No tuition with id: " + id);
            ctx.response.body = {text: 'Invalid tuition id'};
            ctx.response.status = 404;
        } else {
            let tuition = tuitions[index];
            tuitions.splice(index, 1);
            ctx.response.body = tuition;
            ctx.response.status = 200;
        }
    } else {
        ctx.response.body = {text: 'Id missing or invalid'};
        ctx.response.status = 404;
    }
});


const broadcast = (data) =>
    wss.clients.forEach((client) => {
        if (client.readyState === WebSocket.OPEN) {
            client.send(JSON.stringify(data));
        }
    });

router.post('/add', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.request.body;
    // console.log("body: " + JSON.stringify(headers));
    const name = headers.name;
    const description = headers.description;
    const type = headers.type;
    const due = headers.due;
    const status = headers.status;
    const amount = headers.amount;
    if (typeof name !== 'undefined' && typeof description !== 'undefined'
        && typeof type !== 'undefined' && typeof due !== 'undefined' && typeof status !== 'undefined'
        && typeof amount !== 'undefined') {
        const index = tuitions.findIndex(tuition => tuition.name == name);
        if (index !== -1) {
            console.log("Tuition already exists!");
            ctx.response.body = {text: 'Tuition already exists!'};
            ctx.response.status = 404;
        } else {
            let maxId = Math.max.apply(Math, tuitions.map(function (tuition) {
                return tuition.id;
            })) + 1;
            let tuition = {
                id: maxId,
                name,
                description,
                type,
                due,
                status,
                amount
            };
            tuitions.push(tuition);
            broadcast(tuition);
            ctx.response.body = tuition;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: name, description, type, due, status or amount!");
        ctx.response.body = {text: 'Missing or invalid: name, description, type, due, status or amount!'};
        ctx.response.status = 404;
    }
});


app.use(router.routes());
app.use(router.allowedMethods());

server.listen(2220);
