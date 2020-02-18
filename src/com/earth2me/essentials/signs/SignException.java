package com.earth2me.essentials.signs;


public class SignException extends Exception
{
	private static final long serialVersionUID = 1L;

	public SignException(final String message)
	{
		super(message);
	}

	public SignException(final String message, final Throwable throwable)
	{
		super(message, throwable);
	}
}
