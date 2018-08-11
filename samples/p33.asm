* N byte Add (N byte + N byte = N byte)
* Programmed by Akira Uejima

* Data Length(byte)
N:	EQU	0C0H
* Data 1
DATA1:	EQU	80H
* Data 2
DATA2:	EQU	90H
*Result
ANS:	EQU	0A0H

	LD	IX,	[N]
	RCF
LOOP:	LD	ACC,	[IX+DATA1-1]
	ADC	ACC,	[IX+DATA2-1]
	ST	ACC,	[IX+ANS-1]
	SUB	IX,	1
	BP	LOOP
	HLT

	END
