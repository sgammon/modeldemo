
#
#  MODELTESTS DEMO CODEBASE
#  --  pipeline makefile --
#

BUILDBOT ?= 0

all: build
	@echo "Pipeline dependencies ready."

build:
	@echo "Building common logic..."
	@mvn clean package install

clean:
	@echo "Cleaning codebase..."
	@mvn clean
	@echo "Cleaning targets..."
	@rm -frv target/


.PHONY: all build clean

