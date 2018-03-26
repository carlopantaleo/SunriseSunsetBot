const SunCalc = require('suncalc');
const express = require('express');
const app = express();
const port = 8500;

app.use(function (request, response, next) {
    log(`Incoming request: ${request.method} ${request.url}`);
    next();
});

app.get('/json/sun/:lat/:lng/:date', (request, response) => {
    let res = {
        status: "KO",
        message: "",
        results: {}
    };

    if (request.params.lat === undefined || request.params.lng === undefined || request.params.date === undefined) {
        return error(response, "Not all request params specified.");
    }

    let latitude = Number(request.params.lat);
    let longitude = Number(request.params.lng);
    if (isNaN(latitude) || isNaN(longitude)) {
        return error(response, "Latitude or Longitude are not valid numbers.");
    }

    let theDate = new Date(request.params.date);
    if (theDate.toString() === "Invalid Date") {
        return error(response, "Invalid date. Please specify a date in valid ISO format.");
    }

    let results = SunCalc.getTimes(theDate, latitude, longitude);
    success(response, results);
});

app.listen(port, (err) => {
    if (err) {
        return log(`Something bad happened. Error: ${err}`);
    }

    log(`Server is listening on ${port}`);
});

function log(text) {
    console.log(new Date().toISOString().slice(0,19) + " " + text);
}

function error(response, text) {
    let res = {
        status: "KO",
        message: text,
        results: {}
    };

    response.status(400);
    response.json(res);
}

function success(response, results) {
    let res = {
        status: "OK",
        message: "",
        results: results
    };

    response.json(res);
}