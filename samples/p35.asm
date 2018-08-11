* Calculate Greatest Common Divisor using Euclidean Algorithm
* Programmed by Akira Uejima

* A (0 =< A < 128)
A:	EQU	80H
* B (0 < B < 128)
B:	EQU	81H
* GCD(result)
GCD:	EQU	82H

	LD	ACC,	[A]
	LD	IX,	[B]
LOOP:	SUB	ACC,	IX
	BZP	LOOP
	ADD	ACC,	IX
	EOR	IX,	ACC
	EOR	ACC,	IX
	EOR	IX,	ACC
	BNZ	LOOP
	ST	ACC,	[GCD]
	HLT

	END
