const SunCalc = require('suncalc');
const express = require('express');
const app = express();
const port = 8500;

app.use(function (request, response, next) {
    console.log(new Date().toString() +
        ` Incoming request: ${request.method} ${request.url}`);
    next();
});

app.get('/json/sun/:lat/:lng/:date', (request, response) => {
    let res = {
        status: "KO",
        message: "",
        results: {}
    };

    if (request.params.lat === undefined || request.params.lng === undefined || request.params.date === undefined) {
        res.message = "Not all request params specified.";
        response.json(res);
        return;
    }

    let latitude = Number(request.params.lat);
    let longitude = Number(request.params.lng);
    if (isNaN(latitude) || isNaN(longitude)) {
        res.message = "Latitude or Longitude are not valid numbers.";
        response.json(res);
        return;
    }

    let theDate = new Date(request.params.date);

    res.results = SunCalc.getTimes(theDate, latitude, longitude);
    res.status = "OK";
    response.json(res);
});

app.listen(port, (err) => {
    if (err) {
        return console.log('Something bad happened', err);
    }

    console.log(new Date().toString() + ` Server is listening on ${port}`);
});