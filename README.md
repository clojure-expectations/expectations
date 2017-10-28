# Expectations

Visit the expectations website for all of the expectation docs:

<a href="https://clojure-expectations.github.io">https://clojure-expectations.github.io</a>

Running the tests:

    lein do clean, test

This will run the (successful) expectations for Clojure (currently 83).

Then run the ClojureScript tests interactively:

    ./scripts/repl
    cljs.user=> (require 'expectations.test)
    ...
    cljs.user=> (expectations.test/-main)

This will run the (successful) expectations that are compatible with ClojureScript (currently 69/69).

You can run _all_ expectations via:

    lein do clean, expectations

This includes the deliberately failing expectations (used to visually confirm behavior for failing tests) and should run 128 assertions in total, of which 43 will fail and 2 will error.

## License & Copyright

Copyright (c) 2010-2017, Jay C Fields, Sean Corfield. All rights reserved. This software is available under the BSD 3-Clause Open Source License.

## Donate to Jay C Fields, the creator of Expectations

<a href="https://www.coinbase.com/checkouts/7e288c1998b7d7135eeafbe785a2ce60">Donate Bitcoins</a>
