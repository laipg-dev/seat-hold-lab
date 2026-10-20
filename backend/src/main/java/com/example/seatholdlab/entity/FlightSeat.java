package com.example.seatholdlab.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "flight_seats",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_flight_seat",
                columnNames = {"flight_code", "seat_number"}
        )
)
public class FlightSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flight_code", nullable = false, length = 20)
    private String flightCode;

    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatStatus status;

    protected FlightSeat() {
        // Constructor dành cho JPA
    }

    public FlightSeat(String flightCode, String seatNumber) {
        this.flightCode = flightCode;
        this.seatNumber = seatNumber;
        this.status = SeatStatus.AVAILABLE;
    }

    public Long getId() {
        return id;
    }

    public String getFlightCode() {
        return flightCode;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }
}