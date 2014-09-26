# http-test

This program tries to connect to the given URL several times and reports back statistics.

## Usage

    $ java -jar app.jar www.google.com --count 100 --timeout 1000
    Running tests:
            url: www.google.com
    total tests: 100
        timeout: 1000
        
    total: 100, min: 107.68ms, max: 852.94ms, avg: 145.42ms, failed: 3.0%, std-dev: 113.63


## License

Copyright © 2014 Uday Verma

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
