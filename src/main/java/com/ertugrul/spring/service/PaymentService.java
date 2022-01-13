package com.ertugrul.spring.service;


import com.ertugrul.spring.converter.PaymentMapper;
import com.ertugrul.spring.dto.PaymentDto;
import com.ertugrul.spring.entity.Debt;
import com.ertugrul.spring.entity.Payment;
import com.ertugrul.spring.entity.User;
import com.ertugrul.spring.exception.DebtNotFoundException;
import com.ertugrul.spring.exception.PaymentAndDebtAmountNotEqualException;
import com.ertugrul.spring.exception.UserNotFoundException;
import com.ertugrul.spring.service.entityservice.DebtEntityService;
import com.ertugrul.spring.service.entityservice.PaymentEntityService;
import com.ertugrul.spring.service.entityservice.UserEntityService;
import com.ertugrul.spring.util.Constant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final UserEntityService userEntityService;
    private final PaymentEntityService paymentEntityService;
    private final DebtEntityService debtEntityService;


    @Transactional
    public PaymentDto savePayment(PaymentDto paymentDto) {
        Optional<User> userOptional = userEntityService.findById(paymentDto.getUserId());
        if (userOptional.isEmpty())
            throw new UserNotFoundException("User not found.");
        Optional<Debt> debtOptional = debtEntityService.findById(paymentDto.getDebtId());
        if (debtOptional.isEmpty())
            throw new DebtNotFoundException("Debt not found.");
        PaymentDto savedPaymentDto = null;
        Debt debt = debtOptional.get();
        User user = userOptional.get();

        Payment payment = PaymentMapper.INSTANCE.convertPaymentDtoToPayment(paymentDto);

        if (!debt.getTotalAmount().equals(paymentDto.getDebtAmount()))
            throw new PaymentAndDebtAmountNotEqualException("Payment and Debt amounts must be equal.");

        double lateFeeDebt = 0;

        if (debt.getExpiryDate().after(paymentDto.getPaymentDate())) {
            lateFeeDebt = calculateLateFeeDebt(debt);
            Debt lateFeeD = buildLateFeeDebt(user, lateFeeDebt);
            debtEntityService.save(lateFeeD);
        }
        updateCurrentDebt(debt);

        payment.setTotalDebtAmount(lateFeeDebt + debt.getTotalAmount());
        Payment save = paymentEntityService.save(payment);
        savedPaymentDto = PaymentMapper.INSTANCE.convertPaymentDtoToPayment(save);
        return savedPaymentDto;
    }

    private Debt buildLateFeeDebt(User user, double lateFeeDebt) {
        return Debt.builder()
                .expiryDate(new Date())
                .totalAmount(lateFeeDebt)
                .remainingAmount(0.0)
                .userId(user.getId())
                .build();
    }

    private void updateCurrentDebt(Debt debt) {
        debt.setRemainingAmount(0.0);
        debtEntityService.save(debt);
    }

    private double calculateLateFeeDebt(Debt debt) {
        long days = TimeUnit.DAYS.convert(new Date().getTime() - debt.getExpiryDate().getTime(), TimeUnit.MILLISECONDS);
        return days * Constant.getLateFeeRate(debt.getExpiryDate()) / 100;
    }
}