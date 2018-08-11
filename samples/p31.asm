* Sum from 1 to N
* Programmed by Akira Uejima
* N
N:	EQU	80H
* Sum(result)
SUM:	EQU	81H

	LD	IX,	[N]
	LD	ACC,	0
LOOP:	ADD	ACC,	IX
	SUB	IX,	1
	BP	LOOP
	ST	ACC,	[SUM]
	HLT

	END
