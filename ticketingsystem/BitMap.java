package ticketingsystem;

public class BitMap {
    public final Long [][][] bit;
    BitMap(int stationNum, int seatNumEachLong){
        bit = new Long[stationNum][stationNum][seatNumEachLong];
        for(int i = 0; i < stationNum; i++){
            for(int j = i; j < stationNum; j++){
                for(int k = 0; k < seatNumEachLong; k++){
                    bit[i][j][k] = (((0x01L << (j - i)) -1) << i) << (k * (stationNum -1));
                }
            }
        }
    }

    public Long[][][] getBit(){
        return this.bit;
    }

}
