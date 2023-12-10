# Cycling SOAP

## How to run

### Caching Server

First run the caching server.
It is in python so it's easy, just make sure you have the dependencies installed with:
```bash
pip install requests flask
```

Then run python3 main.py

### Routing Server

Then you have to run the routing server, open the solution, compile the file to an exe and run it.

### Java Client

Finally, launch the java client, use maven with the target `javafx:run`.

## Architecture

This application is composed of three elements, these elements are differentiated in order to achieve less responsability for each one.

The `Caching server` is only responsible to retrieve the responses and store them in memory, keeping time of when to delete the record and retrieve new data.

The `Routing server` has only to retrieve data and implement the logic of this application.

Finally the `JavaFX Application has the responsability to display and interact with the user, she doesen't calculate thinks and only sends requests to the `Routing server`.