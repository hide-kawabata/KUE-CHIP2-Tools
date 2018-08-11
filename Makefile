OBJS = ksim.jar kas.jar

all: kas.jar ksim.jar

ksim.jar: ksim.kt
	kotlinc ksim.kt -include-runtime -d ksim.jar 

kas.jar: kas.kt
	kotlinc kas.kt -include-runtime -d kas.jar 

clean:
	rm -f $(OBJS)
