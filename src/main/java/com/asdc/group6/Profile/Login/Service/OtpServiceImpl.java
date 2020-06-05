package com.asdc.group6.Profile.Login.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import org.springframework.stereotype.Component;

import com.asdc.group6.CourseAdmin.DAO.UserDao;
import com.asdc.group6.Models.Otp;
import com.asdc.group6.Models.User;
import com.asdc.group6.Profile.Login.DAO.OtpDao;


@Component
public class OtpServiceImpl implements OtpService {

	private UserDao userDao;
	private MailService mailService;
	private OtpDao otpDao;

	public OtpServiceImpl(UserDao userDao, OtpDao otpDao, MailService mailService) {
		this.userDao = userDao;
		this.otpDao = otpDao;
		this.mailService = mailService;
	}

	@Override
	public String sendOtp(String email) {

		// getting list of user with provided email parameter
		ArrayList<User> allUsers = userDao.getByEmail(email);

		// if no user is found associated with email then send no user found message
		if (allUsers.size() == 0) {
			return "user_not_found";
		}

		// construct new Otp object
		Otp otp = new Otp();
		otp.setEmail(email);

		// generating random four digit number for otp
		Random random = new Random();
		String randomOtp = String.format("%04d", random.nextInt(10000));
		otp.setOtp(Integer.valueOf(randomOtp));

		// set current date and time as the date of creation of otp
		otp.setDate(new Date());

		// sending mail using mail service
		String subject = "Your OTP for password reset";
		String text = "We have received password reset request from your end,\n\n Your otp is " + randomOtp
				+ "\n\n Please use this otp within 10 minutes of it's expiry \n\n Thank You!";

		mailService.sendMail(email, subject, text);

		// insert otp information into otp database table
		otpDao.insertOtp(otp);

		return "success";
	}

	@Override
	public String verifyOtp(String email, String otp, String password) {

		// fetching otp associated to provided email
		ArrayList<Otp> otpList = otpDao.getOtpByEmail(email);
		// get the latest otp generated
		Otp latestOtp = otpList.get(0);
		// get the time when latest otp was generated
		Date latestStamp = latestOtp.getDate();

		// calculate time past 10 minutes from otp generation obtained above
		Calendar current = Calendar.getInstance();
		current.setTime(latestStamp);
		// second argument 10 is minutes after which otp gets expired
		current.add(Calendar.MINUTE, 10);

		// set time 10 minutes past generation of otp
		latestStamp = current.getTime();

		
		// check if time is more than 10 minutes, if so then return response otp_expired
		if (latestStamp.compareTo(new Date()) < 0) {
			System.out.println(new Date());
			return "otp_expired";
		} else {

			// if the user provided otp matches otp stored in database then move forward
			if (Integer.valueOf(otp).equals(latestOtp.getOtp())) {

				ArrayList<User> allUsers = userDao.getByEmail(email);

				// update user password if otp is correct and is not expired
				User userToUpdate = allUsers.get(0);
				userToUpdate.setPassword(password);
				userDao.update(userToUpdate);

				System.out.println("updated");
				return "password_updated";
			} else {
				// if user provided otp does not match then return response otp_invalid
				System.out.println("invalid");
				return "otp_invalid";
			}
		}
	}
}
