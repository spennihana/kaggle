default: elm

elm: web/www/index.js

web/www/index.js: $(wildcard src/main/elm/gui/*elm src/main/elm/gui/*/*elm src/main/elm/gui/Native/*js)
	@echo "compiling " $@ " because " $?
	@cd src/main/elm && elm-package install -y && elm make ./gui/Main.elm --output ../../../web/www/index.js;
	@cd ../../../

.PHONY: clean
clean:
	rm -rf web/www/index.js
