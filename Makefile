.PHONY: build


run:
	mvn exec:java

test:
	mvn test

build:
	mvn compile
