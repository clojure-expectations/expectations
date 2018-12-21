# Expectations

Visit the expectations website for all of the expectation docs:

<a href="https://clojure-expectations.github.io">https://clojure-expectations.github.io</a>

Running the tests:

    lein do clean, test, cljsbuild test

This will run the (successful) expectations for Clojure and ClojureScript (currently 83 and 69 assertions respectively).

You can also run the ClojureScript tests interactively:

    ./scripts/repl
    cljs.user=> (require 'expectations.test)
    ...
    cljs.user=> (expectations.test/-main)

Note: the former "hangs" after completing the ClojureScript tests and, sixty seconds later (agents!), runs a subset of the Clojure tests and fails, so it is currently recommended to do:

    lein do clean, test
    ./scripts/repl
    cljs.user=> (require 'expectations.test)
    ...
    cljs.user=> (expectations.test/-main)

You can run _all_ expectations via:

    lein do clean, expectations

This includes the deliberately failing expectations (used to visually confirm behavior for failing tests) and should run 123 assertions in total, of which 43 will fail and 2 will error.

## Donate to Jay C Fields, the creator of Expectations

<a class="coinbase-button" data-code="7e288c1998b7d7135eeafbe785a2ce60" data-button-style="custom_large" href="https://www.coinbase.com/checkouts/7e288c1998b7d7135eeafbe785a2ce60">Donate Bitcoins</a><script src="https://www.coinbase.com/assets/button.js" type="text/javascript"></script>
