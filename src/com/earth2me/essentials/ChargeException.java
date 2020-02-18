package com.earth2me.essentials;


public class ChargeException extends Exception
{
	private static final long serialVersionUID = 1L;

	public ChargeException(final String message)
	{
		super(message);
	}

	public ChargeException(final String message, final Throwable throwable)
	{
		super(message, throwable);
	}
}
