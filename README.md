# Expectations

Visit the expectations website for all of the expectation docs:

<a href="https://clojure-expectations.github.io">https://clojure-expectations.github.io</a>

Running the tests:

    lein do clean, test-success, test-clojure

This will run the (successful) expectations for Clojure and the (unsuccessful) `clojure.test`-compatible tests for Clojure (currently 83/83 and 15/25 tests/assertions respectively, the latter should show 20 failures and 2 errors).

You can also run the ClojureScript tests interactively:

    ./scripts/repl
    cljs.user=> (require 'expectations.test)
    ...
    cljs.user=> (expectations.test/-main)

This will run the (successful) expectations that are compatible with ClojureScript (currently 69/69).

You can run the "readme" tests:

  lein test-readme

This generates `test/clojure/readme.clj` from `examples.md` and runs those generated tests. You should see 4 tests, with one failure and no errors.

You can run _all_ expectations via:

    lein do clean, expectations

This includes the deliberately failing expectations (used to visually confirm behavior for failing tests) and should run 132 assertions in total, of which 44 will fail and 2 will error.

## License & Copyright

Copyright (c) 2010-2017, Jay C Fields, Sean Corfield. All rights reserved. This software is available under the BSD 3-Clause Open Source License.

## Donate to Jay C Fields, the creator of Expectations

<a href="https://www.coinbase.com/checkouts/7e288c1998b7d7135eeafbe785a2ce60">Donate Bitcoins</a>
