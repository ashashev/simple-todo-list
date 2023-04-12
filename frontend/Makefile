elm_files = $(wildcard src/*.elm)
elm_files += $(wildcard src/**/*.elm)

main.js: $(elm_files)
#	elm make --optimize src/Main.elm --output $@
	elm make src/Main.elm --output $@

.PHONY: fmt,test

fmt:
	elm-format --yes ./src

test:
	elm-test
