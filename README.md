# Saber

(rawr)

## What is it?

Saber allows **live** hacking Obsidian using ClojureScript. While modifying Obsidian was already possible by developing a plugin in ClojureScript and compiling to JS, the process was far from straightforward. Saber looks to make customizing Obsidian via cljs akin to the experience of customizing Emacs using elisp. (Well, more akin, anyway.)

Saber does this through a handful of complementary features:

1. Support runtime evaluation of ClojureScript code powered by [sci](https://github.com/babashka/sci)
2. Load `.cljs` files during Obsidian startup
3. Enable various flavors of ClojureScript code-block evaluation
4. Expose Obsidian features--like command definition--through a cljs-friendly api
5. Provide a built-in nREPL server to support script authoring, debugging, and live hackery
6. [Bonus, soon] Provide a DataScript database of your Obisidian vault 

Saber is very much a work in progress, and its author makes no promises as to its stability, feature-completeness, or his responsiveness to any communiques.

Happy hacking!

## Saber/init.cljs
## ClojureScript code blocks
## nREPL server
## The `saber.obsidian` namespace
