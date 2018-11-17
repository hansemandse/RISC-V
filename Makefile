
all:
	javac Memory.java
	javac IsaSim.java
	java IsaSim

clean:
	rm *.class
