
#
#  MODELTESTS DEMO CODEBASE
#  ---  main makefile   ---
#

BUILDBOT ?= 0

all: build
	@echo "Codebase ready."

dependencies:
	@echo "Installing dependencies..."

build:
	@echo "Building modeltests..."
	@pushd pipeline/ && \
	       $(MAKE) all BUILDBOT=$(BUILDBOT) && popd && \
	       mvn clean package install;
	@echo "Build complete."

deploy:
	@echo "Deploying..."
	@mvn appengine:update
	@echo "Deployment complete."

clean:
	@echo "Cleaning codebase..."
	@mvn clean
	@echo "Cleaning targets..."
	@rm -frv target/ pipeline/target/


.PHONY: all dependencies build deploy clean

