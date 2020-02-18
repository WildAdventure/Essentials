package com.earth2me.essentials.api;

import static com.earth2me.essentials.I18n.tl;

import java.util.UUID;


public class UserDoesNotExistException extends Exception
{
	private static final long serialVersionUID = 1L;

	public UserDoesNotExistException(String name)
	{
		super(tl("userDoesNotExist", name));
	}

	public UserDoesNotExistException(UUID uuid) {
		this(uuid.toString());
	}
}
