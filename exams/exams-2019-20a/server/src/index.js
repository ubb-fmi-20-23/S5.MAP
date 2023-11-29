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

const titles = ['Alita: Battle Angel', 'The Lego Movie 2: The Second Part', 'Isn\'t It Romantic', 'What Men Want', 'Happy Death Day 2U', 'Cold Pursuit', 'The Upside', 'Glass', 'The Prodigy', 'Green Book'];
const descriptions = ['a thing', 'another thing', 'not useful', 'very useful', 'broken'];
const genres = ['adventure', 'action', 'fiction', 'kids', 'romance', 'comedy', 'crime', 'drama'];
const movies = [];
for (let i = 0; i < 20; i++) {
    movies.push({
        id: i + 1,
        title: titles[getRandomInt(0, titles.length)],
        description: descriptions[getRandomInt(0, descriptions.length)],
        genre: genres[getRandomInt(0, genres.length)],
        year: getRandomInt(1950, 2019),
        rating: getRandomInt(1, 5),
        length: getRandomInt(15, 240)
    });
}

const router = new Router();

router.get('/movies', ctx => {
    ctx.response.body = movies;
    ctx.response.status = 200;
});

router.get('/genres', ctx => {
    ctx.response.body = genres;
    ctx.response.status = 200;
});

router.get('/moviesByGenre/:genre', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.params;
    const genre = headers.genre;
    if (typeof genre !== 'undefined') {
        ctx.response.body = movies.filter(movie => movie.genre == genre);
        ctx.response.status = 200;
    } else {
        console.log("Missing or invalid: genre!");
        ctx.response.body = {text: 'Missing or invalid: genre!'};
        ctx.response.status = 404;
    }
});

router.get('/details/:id', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.params;
    const id = headers.id;
    if (typeof id !== 'undefined') {
        const index = movies.findIndex(movie => movie.id == id);
        if (index === -1) {
            console.log("Movie not available!");
            ctx.response.body = {text: 'Movie not available!'};
            ctx.response.status = 404;
        } else {
            let movie = movies[index];
            ctx.response.body = movie;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: id!");
        ctx.response.body = {text: 'Missing or invalid: id!'};
        ctx.response.status = 404;
    }
});

router.post('/update', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.request.body;
    // console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    const title = headers.title;
    const description = headers.description;
    const genre = headers.genre;
    const year = headers.year;
    const rating = headers.rating;
    const length = headers.length;
    if (typeof id !== 'undefined' && typeof title !== 'undefined' && typeof description !== 'undefined'
        && typeof genre !== 'undefined' && typeof year !== 'undefined' && typeof rating !== 'undefined'
        && typeof length !== 'undefined') {
        const index = movies.findIndex(movie => movie.id == id);
        if (index === -1) {
            console.log("Movie not available!");
            ctx.response.body = {text: 'Movie not available!'};
            ctx.response.status = 404;
        } else {
            let movie = movies[index];
            movie.title = title;
            movie.description = description;
            movie.genre = genre;
            movie.year = year;
            movie.rating = rating;
            movie.length = length;
            ctx.response.body = movie;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: id, title, description, genre, year, rating or length!");
        ctx.response.body = {text: 'Missing or invalid: id, title, description, genre, year, rating or length!'};
        ctx.response.status = 404;
    }
});

router.post('/updateDescription', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.request.body;
    // console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    const description = headers.description;
    if (typeof id !== 'undefined' && typeof description !== 'undefined') {
        const index = movies.findIndex(movie => movie.id == id);
        if (index === -1) {
            console.log("Movie not available!");
            ctx.response.body = {text: 'Movie not available!'};
            ctx.response.status = 404;
        } else {
            let movie = movies[index];
            movie.description = description;
            ctx.response.body = movie;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: id or description!");
        ctx.response.body = {text: 'Missing or invalid: id or description!'};
        ctx.response.status = 404;
    }
});

router.post('/updateRating', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.request.body;
    // console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    const rating = headers.rating;
    if (typeof id !== 'undefined' && typeof rating !== 'undefined') {
        const index = movies.findIndex(movie => movie.id == id);
        if (index === -1) {
            console.log("Movie not available!");
            ctx.response.body = {text: 'Movie not available!'};
            ctx.response.status = 404;
        } else {
            let movie = movies[index];
            movie.rating = rating;
            ctx.response.body = movie;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: id or rating!");
        ctx.response.body = {text: 'Missing or invalid: id or rating!'};
        ctx.response.status = 404;
    }
});


router.del('/delete/:id', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.params;
    // console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    if (typeof id !== 'undefined') {
        const index = movies.findIndex(movie => movie.id == id);
        if (index === -1) {
            console.log("No movie with id: " + id);
            ctx.response.body = {text: 'Invalid movie id'};
            ctx.response.status = 404;
        } else {
            let movie = movies[index];
            movies.splice(index, 1);
            ctx.response.body = movie;
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
    const title = headers.title;
    const description = headers.description;
    const genre = headers.genre;
    const year = headers.year;
    const rating = headers.rating;
    const length = headers.length;
    if (typeof title !== 'undefined' && typeof description !== 'undefined'
        && typeof genre !== 'undefined' && typeof year !== 'undefined' && typeof rating !== 'undefined'
        && typeof length !== 'undefined') {
        const index = movies.findIndex(movie => movie.title == title);
        if (index !== -1) {
            console.log("Movie already exists!");
            ctx.response.body = {text: 'Movie already exists!'};
            ctx.response.status = 404;
        } else {
            let maxId = Math.max.apply(Math, movies.map(function (movie) {
                return movie.id;
            })) + 1;
            let movie = {
                id: maxId,
                title,
                description,
                genre,
                year,
                rating,
                length
            };
            movies.push(movie);
            broadcast(movie);
            ctx.response.body = movie;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: title, description, genre, year, rating or length!");
        ctx.response.body = {text: 'Missing or invalid: title, description, genre, year, rating or length!'};
        ctx.response.status = 404;
    }
});

router.del('/bookmark/:id', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.params;
    // console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    if (typeof id !== 'undefined') {
        const index = movies.findIndex(bookmark => bookmark.id == id);
        if (index === -1) {
            console.log("No bookmark with id: " + id);
            ctx.response.body = {text: 'Invalid bookmark id'};
            ctx.response.status = 404;
        } else {
            let bookmark = movies[index];
            movies.splice(index, 1);
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

server.listen(2020);
