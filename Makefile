
#
#  MODELTESTS DEMO CODEBASE
#  ---  main makefile   ---
#


#
## Environment
#

null :=
period := ${null}.${null}
hyphen := ${null}-${null}

BUILDBOT ?= 0
ENV ?= .env/
SOFTWARE_STORAGE ?= https://storage.googleapis.com/mm-internal/software

PEBBLE_VERSION ?= 2.0.2-SNAPSHOT


#
## Build Flow
#

all: build
	@echo "Codebase ready."

dependencies: $(ENV)java/lib/pebble-$(PEBBLE_VERSION).jar
	@echo "Dependencies ready."

build: dependencies
	@echo "Building modeltests..."
	@cd pipeline/ && \
	       $(MAKE) all BUILDBOT=$(BUILDBOT) && cd .. && \
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


#
## Dependencies
#

$(ENV)java/lib/pebble-$(PEBBLE_VERSION).jar:
	@echo "Installing Pebble v$(PEBBLE_VERSION)..."
	@mkdir -p $(ENV)java/lib/
	@curl --progress-bar $(SOFTWARE_STORAGE)/pebble-$(subst ${period},${hyphen},${PEBBLE_VERSION}).jar > $(ENV)java/lib/pebble-$(PEBBLE_VERSION).jar
	@echo "Installing Pebble v$(PEBBLE_VERSION)..."
	@mvn install:install-file -Dfile=$(ENV)java/lib/pebble-$(PEBBLE_VERSION).jar -DgroupId=com.mitchellbosecke -DartifactId=pebble -Dversion=$(PEBBLE_VERSION) -Dpackaging=jar;
	@echo "Installation complete."


.PHONY: all dependencies build deploy clean

