package revoluttest;

public class TransferReqBean {

	public TransferReqBean() {
	}
	
	public TransferReqBean(long from, long to, long amount) {
		this.from = from;
		this.to = to;
		this.amount = amount;
	}

	private long from;
	private long to;
	private long amount;
	
	public long getFrom() {
		return from;
	}

	public long getTo() {
		return to;
	}

	public long getAmount() {
		return amount;
	}

	public void setFrom(long from) {
		this.from = from;
	}

	public void setTo(long to) {
		this.to = to;
	}

	public void setAmount(long amount) {
		this.amount = amount;
	}

}
