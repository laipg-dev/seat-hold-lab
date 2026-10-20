package com.example.seatholdlab.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "seat_holds")
public class SeatHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flight_seat_id", nullable = false)
    private FlightSeat flightSeat;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HoldStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    protected SeatHold() {
        // Constructor dành cho JPA
    }

    public SeatHold(
            FlightSeat flightSeat,
            String customerName,
            Instant createdAt,
            Instant expiresAt
    ) {
        this.flightSeat = flightSeat;
        this.customerName = customerName;
        this.status = HoldStatus.PENDING;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public FlightSeat getFlightSeat() {
        return flightSeat;
    }

    public String getCustomerName() {
        return customerName;
    }

    public HoldStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setStatus(HoldStatus status) {
        this.status = status;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }
}