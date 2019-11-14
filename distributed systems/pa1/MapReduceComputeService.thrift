
service MapReduceComputeService {
	bool ping(),
	string getSent(1: string filename),
	string sortSent()
}
