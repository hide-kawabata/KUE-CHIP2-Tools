kuesym.jar: kuesym.kt
	kotlinc kuesym.kt -include-runtime -d kuesym.jar 

kas.jar: kas.kt
	kotlinc kas.kt -include-runtime -d kas.jar 
