remote-dev:
	ssh -R 0.0.0.0:4568:localhost:4567 wolfia -N

remte-debug:
	ssh -L 5005:localhost:5005 wolfia -N
