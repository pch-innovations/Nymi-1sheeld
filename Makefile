all: build  

build: clean
	 ./gradlew build;

clean:
	./gradlew clean