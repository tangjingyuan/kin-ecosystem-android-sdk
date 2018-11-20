package com.kin.ecosystem.core.accountmanager;

import android.support.annotation.NonNull;
import com.kin.ecosystem.common.KinCallback;
import com.kin.ecosystem.common.ObservableData;
import com.kin.ecosystem.common.Observer;
import com.kin.ecosystem.common.exception.BlockchainException;
import com.kin.ecosystem.common.exception.KinEcosystemException;
import com.kin.ecosystem.core.Log;
import com.kin.ecosystem.core.Logger;
import com.kin.ecosystem.core.bi.EventLogger;
import com.kin.ecosystem.core.bi.events.StellarAccountCreationRequested;
import com.kin.ecosystem.core.bi.events.WalletCreationSucceeded;
import com.kin.ecosystem.core.data.auth.AuthDataSource;
import com.kin.ecosystem.core.data.blockchain.BlockchainSource;
import com.kin.ecosystem.core.network.model.AuthToken;
import com.kin.ecosystem.core.util.ErrorUtil;
import kin.core.EventListener;
import kin.core.KinAccount;
import kin.core.ListenerRegistration;

public class AccountManagerImpl implements AccountManager {

	private static final String TAG = AccountManagerImpl.class.getSimpleName();

	private static volatile AccountManagerImpl instance;

	private final AccountManager.Local local;
	private final EventLogger eventLogger;
	private AuthDataSource authRepository;
	private BlockchainSource blockchainSource;
	private final ObservableData<Integer> accountState;
	private KinEcosystemException error;

	private ListenerRegistration accountCreationRegistration;

	private AccountManagerImpl(@NonNull final AccountManager.Local local,
		@NonNull final EventLogger eventLogger,
		@NonNull final AuthDataSource authRepository,
		@NonNull final BlockchainSource blockchainSource) {
		this.local = local;
		this.eventLogger = eventLogger;
		this.authRepository = authRepository;
		this.blockchainSource = blockchainSource;
		this.accountState = ObservableData.create(local.getAccountState());
	}

	public static void init(@NonNull final AccountManager.Local local,
		@NonNull final EventLogger eventLogger,
		@NonNull final AuthDataSource authRepository,
		@NonNull final BlockchainSource blockchainSource) {
		if (instance == null) {
			synchronized (AccountManagerImpl.class) {
				if (instance == null) {
					instance = new AccountManagerImpl(local, eventLogger, authRepository, blockchainSource);
				}
			}
		}
	}

	public static AccountManager getInstance() {
		return instance;
	}

	@Override
	public @AccountState
	int getAccountState() {
		return accountState.getValue() == ERROR ? ERROR : local.getAccountState();
	}

	@Override
	public boolean isAccountCreated() {
		return local.getAccountState() == CREATION_COMPLETED;
	}

	@Override
	public void addAccountStateObserver(@NonNull Observer<Integer> observer) {
		accountState.addObserver(observer);
		accountState.postValue(accountState.getValue());
	}

	@Override
	public void removeAccountStateObserver(@NonNull Observer<Integer> observer) {
		accountState.removeObserver(observer);
	}

	@Override
	public KinEcosystemException getError() {
		return error;
	}

	@Override
	public void retry() {
		if (getKinAccount() != null && accountState.getValue() == ERROR) {
			this.setAccountState(local.getAccountState());
		}
	}

	@Override
	public void start() {
		if (getKinAccount() != null) {
			Logger.log(new Log().withTag(TAG).put("setAccountState", "start"));
			if (getAccountState() != CREATION_COMPLETED) {
				this.setAccountState(local.getAccountState());
			}
		}
	}

	private void setAccountState(@AccountState final int accountState) {
		if (isValidState(this.accountState.getValue(), accountState)) {
			if (accountState != ERROR) {
				this.local.setAccountState(accountState);
			}
			this.accountState.postValue(accountState);
			switch (accountState) {
				case REQUIRE_CREATION:
					eventLogger.send(StellarAccountCreationRequested.create());
					Logger.log(new Log().withTag(TAG).put("setAccountState", "REQUIRE_CREATION"));
					// Trigger account creation from server side.
					authRepository.getAuthToken(new KinCallback<AuthToken>() {
						@Override
						public void onResponse(AuthToken response) {
							setAccountState(PENDING_CREATION);
						}

						@Override
						public void onFailure(KinEcosystemException error) {
							instance.error = error;
							setAccountState(ERROR);
						}
					});
					break;
				case PENDING_CREATION:
					Logger.log(new Log().withTag(TAG).put("setAccountState", "PENDING_CREATION"));
					// Start listen for account creation on the blockchain side.
					if (accountCreationRegistration != null) {
						removeAccountCreationRegistration();
					}
					accountCreationRegistration = getKinAccount().blockchainEvents()
						.addAccountCreationListener(new EventListener<Void>() {
							@Override
							public void onEvent(Void data) {
								removeAccountCreationRegistration();
								setAccountState(REQUIRE_TRUSTLINE);
							}
						});
					break;
				case REQUIRE_TRUSTLINE:
					Logger.log(new Log().withTag(TAG).put("setAccountState", "REQUIRE_TRUSTLINE"));
					// Create trustline transaction with KIN
					blockchainSource.createTrustLine(new KinCallback<Void>() {
						@Override
						public void onResponse(Void response) {
							setAccountState(CREATION_COMPLETED);
						}

						@Override
						public void onFailure(KinEcosystemException error) {
							instance.error = error;
							setAccountState(ERROR);
						}
					});
					break;
				case CREATION_COMPLETED:
					// Mark account creation completed.
					eventLogger.send(WalletCreationSucceeded.create());
					Logger.log(new Log().withTag(TAG).put("setAccountState", "CREATION_COMPLETED"));
					break;
				default:
				case AccountManager.ERROR:
					Logger.log(new Log().withTag(TAG).put("setAccountState", "ERROR"));
					break;

			}
		}
	}

	@Override
	public void switchAccount(final int accountIndex, @NonNull final KinCallback<Boolean> callback) {
		Logger.log(new Log().withTag(TAG).put("switchAccount", "start"));
		final String address = blockchainSource.getPublicAddress(accountIndex);
		//update sign in data with new wallet address and update servers
		authRepository.updateWalletAddress(address, new KinCallback<Boolean>() {
			@Override
			public void onResponse(Boolean response) {
				try {
					//switch to the new KinAccount
					blockchainSource.updateActiveAccount(accountIndex);
				} catch (BlockchainException e) {
					callback.onFailure(ErrorUtil.getBlockchainException(e));
					return;
				}
				Logger.log(new Log().withTag(TAG).put("switchAccount", "ended successfully"));
				callback.onResponse(response);
			}

			@Override
			public void onFailure(KinEcosystemException exception) {
				//switch to the new KinAccount
				callback.onFailure(exception);
				Logger.log(new Log().withTag(TAG).put("switchAccount", "ended with failure"));
			}
		});
	}

	private KinAccount getKinAccount() {
		return blockchainSource.getKinAccount();
	}

	private void removeAccountCreationRegistration() {
		accountCreationRegistration.remove();
		accountCreationRegistration = null;
	}

	private boolean isValidState(int currentState, int newState) {
		return newState == ERROR || currentState == ERROR || newState >= currentState
			//allow recreating an account in case of switching account after restore
			|| (currentState == CREATION_COMPLETED && newState == REQUIRE_CREATION);
	}

}
