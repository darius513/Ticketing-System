package ticketingsystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.lang.Thread.sleep;

public class TicketingDS implements TicketingSystem {

    private static AtomicLong ticketId;
//    private List<TrainImpl1> trainList;
    private List<TrainImpl2> trainList;

    private static ThreadLocal<List<Ticket>> soldTicket_THREADLOCAL = ThreadLocal.withInitial(ArrayList::new);

    private static AtomicLong slot = new AtomicLong(1);
    private static ThreadLocal<Long> intervalBegin = ThreadLocal.withInitial(() -> 0L);

    private static ThreadLocal<Long> intervalEnd = ThreadLocal.withInitial(() -> 0L);

    private static ThreadLocal<Long> nowThreadId = ThreadLocal.withInitial(() -> 1L);

    ThreadLocal<Ticket[]> releaseBuffer = ThreadLocal.withInitial(()->new Ticket[10]);
    ThreadLocal<Integer> next = ThreadLocal.withInitial(()-> 0);

    ThreadLocal<Random> localRandom = ThreadLocal.withInitial(()->new Random(System.nanoTime()));

    private void addBuffer(Ticket ticket){
        int n = next.get();
        releaseBuffer.get()[n] = ticket;
        next.set((n+1) % 10);
    }

    private int searchReleaseBuffer(int departure, int arrival, int route){
        Ticket[] tickets = releaseBuffer.get();
        int seat = -1;
        for (int i = 0; i < tickets.length; i++) {
            if (tickets[i] != null && tickets[i].route == route && tickets[i].departure <= departure && tickets[i].arrival >= arrival) {
                seat = tickets[i].seat + trainList.get(route - 1).seatNumOfEachCoach * (tickets[i].coach - 1) - 1;
                releaseBuffer.get()[i] = null;
                break;
            }
        }
        return seat;
    }





    public TicketingDS(int routenum, int coachnum, int seatnum, int stationnum, int threadnum) {
        trainList = new ArrayList<>();
        for(int i = 0; i<routenum; i++){
//            trainList.add(new TrainImpl1(coachnum, seatnum, stationnum));
            trainList.add(new TrainImpl2(coachnum, seatnum, stationnum));
        }
        ticketId = new AtomicLong(1);
    }

    public long searchThreadId(){
        if(nowThreadId.get() > intervalEnd.get()){
            intervalBegin.set(slot.addAndGet(100000));
            intervalEnd.set(intervalBegin.get()+99999);
            nowThreadId.set(intervalBegin.get());
        }
        long ThreadId = nowThreadId.get();
        nowThreadId.set(ThreadId + 1);
        return ThreadId;
    }

    @Override
    public Ticket buyTicket(String passenger, int route, int departure, int arrival)  {
        //占座

        int seat = 0;
//        int releaseSeat = searchReleaseBuffer(departure, arrival, route);
//        int releaseSeat = new Random().nextInt(trainList.get(route-1).totalSeatNum);
//        int releaseSeat = localRandom.get().nextInt(trainList.get(route-1).totalSeatNum);
        int releaseSeat = 0;
//        if(releaseSeat == -1){
////            releaseSeat = localRandom.get().nextInt(trainList.get(route-1).totalSeatNum);
//            releaseSeat = (int) (Thread.currentThread().getId() % trainList.get(route-1).coachnum) * trainList.get(route-1).seatNumOfEachCoach;
//        }
        try {
            seat = trainList.get(route-1).reserveSeat(departure-1, arrival-1, releaseSeat);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(seat == -1) return null;
        Ticket ticket = new Ticket();

        //不用atomic
        ticket.tid = searchThreadId();
//        ticket.tid = ticketId.getAndIncrement();
        ticket.seat = (seat % trainList.get(route-1).seatNumOfEachCoach) + 1;
        ticket.coach = (seat / trainList.get(route-1).seatNumOfEachCoach) + 1;
        ticket.passenger = passenger;
        ticket.departure = departure;
        ticket.arrival = arrival;
        ticket.route = route;
        //加入已经卖出的票中
        List<Ticket> list = soldTicket_THREADLOCAL.get();
        list.add(ticket);
        soldTicket_THREADLOCAL.set(list);

        return ticket;
    }

    @Override
    public int inquiry(int route, int departure, int arrival) {
        return trainList.get(route-1).remainSeat(departure-1, arrival-1);
    }


    @Override
    public boolean refundTicket(Ticket ticket) {
        //检查是否卖了这张票
        if(!soldTicket_THREADLOCAL.get().contains(ticket)){
            return false;
        }
        //无锁的已买票的判断
        int seat = trainList.get(ticket.route-1).seatNumOfEachCoach * (ticket.coach - 1) + ticket.seat - 1;
        try {
            if(trainList.get(ticket.route-1).releaseSeat(seat, ticket.departure-1, ticket.arrival-1)){
                List<Ticket>list =  soldTicket_THREADLOCAL.get();
                list.remove(ticket);
                soldTicket_THREADLOCAL.set(list);
//                addBuffer(ticket);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }
}
