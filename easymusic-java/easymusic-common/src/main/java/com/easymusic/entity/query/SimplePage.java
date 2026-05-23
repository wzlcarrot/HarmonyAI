package com.easymusic.entity.query;
import com.easymusic.entity.enums.PageSize;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SimplePage {
	private int pageNo;  //当前页
	private int countTotal;	//总记录数
	private int pageSize;	//每页记录数
	private int pageTotal;  //总页数
	private int start;    //起始索引
	private int end;   //偏移量

	public SimplePage(Integer pageNo, int countTotal, int pageSize) {
		if (pageNo == null) pageNo = 0;
		this.pageNo = pageNo;
		this.countTotal = countTotal;
		this.pageSize = pageSize;
		action();
	}

	public SimplePage(int start, int end) {
		this.start = start;
		this.end = end;
	}

	public void action() {

		//主要是为了防止pageSize小于等于0，从而导致sql出现limit -1,0这种错误。
		//当出现这种错误，则将pageSize设置为20
		if (this.pageSize <= 0) {
			this.pageSize = PageSize.SIZE20.getSize();
		}
		//得到总页数
		if (this.countTotal > 0) {
			this.pageTotal = this.countTotal % this.pageSize == 0 ? this.countTotal / this.pageSize
					: this.countTotal / this.pageSize + 1;
		} else {
			pageTotal = 1;
		}
		//如果当前页码小于1，则当前页码为1
		if (pageNo <= 1) {
			pageNo = 1;
		}
		//如果当前页码大于总页数，则当前页码为总页数
		if (pageNo > pageTotal) {
			pageNo = pageTotal;
		}

		this.start = (pageNo - 1) * pageSize;  //开始索引

		this.end = this.pageSize;	//偏移量
	}

}
