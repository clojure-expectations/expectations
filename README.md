# Expectations

Visit the expectations website for all of the expectation docs:

<a href="https://clojure-expectations.github.io">https://clojure-expectations.github.io</a>

Running the tests:

    lein do clean, test-success, test-clojure

This will run the (successful) expectations for Clojure and the (unsuccessful) `clojure.test`-compatible tests for Clojure (currently 82/82 and 11/20 tests/assertions respectively, the latter should show 16 failures and 4 errors).

You can also run the ClojureScript tests interactively:

    ./scripts/repl
    cljs.user=> (require 'expectations.test)
    ...
    cljs.user=> (expectations.test/-main)

This will run the (successful) expectations that are compatible with ClojureScript (currently 68/68).

You can run _all_ expectations via:

    lein do clean, expectations

This includes the deliberately failing expectations (used to visually confirm behavior for failing tests) and should run 127 assertions in total, of which 43 will fail and 2 will error.

## Donate to Jay C Fields, the creator of Expectations

<a class="coinbase-button" data-code="7e288c1998b7d7135eeafbe785a2ce60" data-button-style="custom_large" href="https://www.coinbase.com/checkouts/7e288c1998b7d7135eeafbe785a2ce60">Donate Bitcoins</a><script src="https://www.coinbase.com/assets/button.js" type="text/javascript"></script>
