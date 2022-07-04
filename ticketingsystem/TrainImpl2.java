package ticketingsystem;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 多座位状态位图实现
 */
public class TrainImpl2 {
    public int coachnum;
    public int seatNumOfEachCoach;
    public int totalSeatNum;
    public int stationnum;
    public int intervalNum;
    public int NumLong;     //一共需要多少个Long
    public int seatNumEachLong; //每个Long能存储多少个座位的状态
    public int ScrapSeatNum; //边角料    需要另外用一个Long存储的剩余几个座位的状态,为0表示所有Long正好存满
    //存储每个座位的占座情况，每个Long用位图表示该座位在不同station的占座情况。
    private AtomicLong[] seatState;
    //存储每个车厢占座情况，越大表示该车厢人越多
    //不用Atomic，是想这个占座密度可以不用那么准确，用Atomic会降低性能
    private Integer [] coachState;
//    private RemainTickets remainTickets;
//    private LocalRemainTicket localRemainTicket;
//        public BitMap bitMap;



    TrainImpl2(int coachnum, int seatnum, int stationnum){
        this.totalSeatNum = coachnum * seatnum;
        this.intervalNum = stationnum - 1;
        this.seatNumEachLong = 64 / (stationnum - 1);
        this.ScrapSeatNum = totalSeatNum % seatNumEachLong;
        this.NumLong = ScrapSeatNum == 0 ? totalSeatNum / seatNumEachLong : totalSeatNum / seatNumEachLong + 1;
        this.coachnum = coachnum;
        this.seatNumOfEachCoach = seatnum;

        this.stationnum = stationnum;
        this.seatState = new AtomicLong[NumLong];
        this.coachState = new Integer[coachnum];
        for(int i=0; i < this.NumLong; i++){
            this.seatState[i] = new AtomicLong(0);
        }
        for(int i=0;i<this.coachnum;i++){
            this.coachState[i] = 0;
        }

//        bitMap = new BitMap(stationnum, 64 / (stationnum - 1));

    }

    /**
     * 查找人最少的车厢
     * @return 车厢号
     */
    public int findColdCoach(){
        int min = 0;
        int ColdCoach = 0;
        for(int i = 0; i<this.coachState.length;i++){
            if(min > this.coachState[i]){
                min = this.coachState[i];
                ColdCoach = i;
            }
        }
        return ColdCoach;
    }

    /**
     * 创建掩码
     * @param departure 始发站
     * @param arrival 终点站
     * @return 掩码
     */
    public Long makeMask(int departure, int arrival){
        return ((0x01L << (arrival - departure)) -1) << departure;
    }

    /**
     * 占座
     * @param departure 始发站
     * @param arrival 终点站
     * @return 车票/空
     */
    public int reserveSeat(int departure, int arrival, int beginSeat) throws InterruptedException {
        long mask = makeMask(departure, arrival);
        long maskTemp;


        for(int i = 0;i<this.seatState.length;i++){
            long State = seatState[i].get();    //一次拿到多个座位的状态
            int seatNum = this.seatNumEachLong;
            if(i == this.seatState.length - 1){ //拿到的是边角料
                seatNum = this.ScrapSeatNum;
            }
            for(int j = 0; j < seatNum; j++){ //一个座位一个座位地尝试
                maskTemp = mask << (j * this.intervalNum);
//                maskTemp = bitMap.getBit()[departure][arrival][j];
                while((maskTemp & State) == 0){
                    if(seatState[i].compareAndSet(State, (State | maskTemp))){
                        return i * this.seatNumEachLong + j;
                    }
                    State = seatState[i].get();
                }
            }
        }
        return -1;
    }


    public boolean releaseSeat(int seat, int departure, int arrival) throws InterruptedException {
        int seatStateIndex = seat / this.seatNumEachLong;
        int LongIndex = seat % this.seatNumEachLong;
        long mask = makeMask(departure, arrival) << (LongIndex * this.intervalNum);
//        long mask = bitMap.getBit()[departure][arrival][LongIndex];
        long seatState = this.seatState[seatStateIndex].get();
        while(true) {
            if (this.seatState[seatStateIndex].compareAndSet(seatState, (seatState & ~ mask))) {
                return true;
            }
            seatState = this.seatState[seatStateIndex].get();
        }
    }

    public int remainSeat(int departure, int arrival) {
        long mask = makeMask(departure, arrival);
//        long mask;
        int remainSeat = 0;
        int seatStateLen = this.seatState.length;
        if(this.ScrapSeatNum != 0){ //处理边角料
            long ScrapSeatsState = this.seatState[seatStateLen-1].get();
            for(int j = 0; j < this.ScrapSeatNum; j++){
//                mask = bitMap.getBit()[departure][arrival][j];
                if((ScrapSeatsState & (mask << (j * this.intervalNum))) == 0){
//                if((ScrapSeatsState & (mask)) == 0){
                    remainSeat++;
                }
            }
            seatStateLen--;
        }

        for(int i = 0; i < seatStateLen; i++){
            long state = this.seatState[i].get();
            for(int j = 0; j < seatNumEachLong; j++){ //先遍历
//                mask = bitMap.getBit()[departure][arrival][j];
                if((state & (mask << (j * this.intervalNum))) == 0){
//                if((state & (mask)) == 0){
                    remainSeat++;
                }
            }
        }
        return remainSeat;

    }
}
