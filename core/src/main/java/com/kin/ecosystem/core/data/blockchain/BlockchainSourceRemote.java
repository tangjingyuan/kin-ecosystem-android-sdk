package com.kin.ecosystem.core.data.blockchain;

import android.support.annotation.NonNull;
import com.kin.ecosystem.common.Callback;
import com.kin.ecosystem.common.exception.BlockchainException;
import com.kin.ecosystem.core.bi.EventLogger;
import com.kin.ecosystem.core.bi.events.MigrationBCVersionCheckFailed;
import com.kin.ecosystem.core.bi.events.MigrationBCVersionCheckSucceeded;
import com.kin.ecosystem.core.bi.events.MigrationStatusCheckFailed;
import com.kin.ecosystem.core.bi.events.MigrationStatusCheckSucceeded;
import com.kin.ecosystem.core.bi.events.MigrationStatusCheckSucceeded.IsRestorable;
import com.kin.ecosystem.core.bi.events.MigrationStatusCheckSucceeded.ShouldMigrate;
import com.kin.ecosystem.core.data.blockchain.BlockchainSource.Remote;
import com.kin.ecosystem.core.network.ApiCallback;
import com.kin.ecosystem.core.network.ApiException;
import com.kin.ecosystem.core.network.api.MigrationApi;
import com.kin.ecosystem.core.network.model.MigrationInfo;
import com.kin.ecosystem.core.util.ExecutorsUtil;
import java.util.List;
import java.util.Map;
import kin.sdk.migration.common.KinSdkVersion;

public class BlockchainSourceRemote implements Remote {
	private static volatile BlockchainSourceRemote instance;

	private EventLogger eventLogger;
	private MigrationApi migrationApi;
	private ExecutorsUtil executorsUtil;

	private BlockchainSourceRemote(@NonNull ExecutorsUtil executorsUtil, @NonNull EventLogger eventLogger) {
		this.eventLogger = eventLogger;
		this.migrationApi = new MigrationApi();
		this.executorsUtil = executorsUtil;
	}

	public static BlockchainSourceRemote getInstance(@NonNull ExecutorsUtil executorsUtil, @NonNull EventLogger eventLogger) {
		if (instance == null) {
			synchronized (BlockchainSourceRemote.class) {
				if (instance == null) {
					instance = new BlockchainSourceRemote(executorsUtil, eventLogger);
				}
			}
		}
		return instance;
	}

	@Override
	public KinSdkVersion getBlockchainVersion() throws ApiException {
		try {
			String version = migrationApi.getBlockchainVersionSync("");
			MigrationBCVersionCheckSucceeded.BlockchainVersion bcVersion;
			try {
				bcVersion = MigrationBCVersionCheckSucceeded.BlockchainVersion.fromValue(version);
			} catch (Exception e) {
				bcVersion = null;
			}

			eventLogger.send(MigrationBCVersionCheckSucceeded.create(getPublicAddress(), bcVersion));
			return KinSdkVersion.get(version);
		} catch (ApiException e) {
			MigrationBCVersionCheckFailed.BlockchainVersion bcVersion;
			try {
				bcVersion = MigrationBCVersionCheckFailed.BlockchainVersion.fromValue(BlockchainSourceImpl.getInstance().getBlockchainVersion().getVersion());
			} catch (Exception e2) {
				bcVersion = null;
			}

			eventLogger.send(MigrationBCVersionCheckFailed.create(
				getErrorMessage(e),
				getPublicAddress(),
				bcVersion
			));

			throw e;
		}
	}

	@Override
	public void getBlockchainVersion(@NonNull final Callback<KinSdkVersion, ApiException> callback) {
		try {
			migrationApi.getBlockchainVersionAsync("", new ApiCallback<String>() {
				@Override
				public void onFailure(final ApiException e, final int statusCode, final Map<String, List<String>> responseHeaders) {
					MigrationBCVersionCheckFailed.BlockchainVersion bcVersion;
					try {
						bcVersion = MigrationBCVersionCheckFailed.BlockchainVersion.fromValue(BlockchainSourceImpl.getInstance().getBlockchainVersion().getVersion());
					} catch (Exception e2) {
						bcVersion = null;
					}
					eventLogger.send(MigrationBCVersionCheckFailed.create(
						getErrorMessage(e),
						getPublicAddress(),
						bcVersion
					));
					executorsUtil.mainThread().execute(new Runnable() {
						@Override
						public void run() {
							callback.onFailure(e);
						}
					});
				}

				@Override
				public void onSuccess(final String result, final int statusCode, final Map<String, List<String>> responseHeaders) {
					MigrationBCVersionCheckSucceeded.BlockchainVersion bcVersion;
					try {
						bcVersion = MigrationBCVersionCheckSucceeded.BlockchainVersion.fromValue(result);
					} catch (Exception e) {
						bcVersion = null;
					}

					eventLogger.send(MigrationBCVersionCheckSucceeded.create(getPublicAddress(), bcVersion));
					executorsUtil.mainThread().execute(new Runnable() {
						@Override
						public void run() {
							callback.onResponse(KinSdkVersion.get(result));
						}
					});
				}
			});
		} catch (final ApiException e) {
			MigrationBCVersionCheckFailed.BlockchainVersion bcVersion;
			try {
				bcVersion = MigrationBCVersionCheckFailed.BlockchainVersion.fromValue(BlockchainSourceImpl.getInstance().getBlockchainVersion().getVersion());
			} catch (Exception e2) {
				bcVersion = null;
			}

			eventLogger.send(MigrationBCVersionCheckFailed.create(
				getErrorMessage(e),
				getPublicAddress(),
				bcVersion
			));
			executorsUtil.mainThread().execute(new Runnable() {
				@Override
				public void run() {
					callback.onFailure(e);
				}
			});
		}
	}

	@Override
	public MigrationInfo getMigrationInfo(@NonNull String publicAddress) throws ApiException {
		try {
			final MigrationInfo info = migrationApi.getMigrationInfoSync(publicAddress);
			sendMigrationInfoSuccessEvent(publicAddress, info);
			return info;
		} catch (ApiException e) {
			eventLogger.send(MigrationStatusCheckFailed.create(getPublicAddress(), getErrorMessage(e)));
			throw e;
		}
	}

	@Override
	public void getMigrationInfo(final String publicAddress, final @NonNull Callback<MigrationInfo, ApiException> callback) {
		try {
			migrationApi.getMigrationInfoAsync(publicAddress, new ApiCallback<MigrationInfo>() {
				@Override
				public void onFailure(final ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
					eventLogger.send(MigrationStatusCheckFailed.create(getPublicAddress(), getErrorMessage(e)));
					executorsUtil.mainThread().execute(new Runnable() {
						@Override
						public void run() {
							callback.onFailure(e);
						}
					});
				}

				@Override
				public void onSuccess(final MigrationInfo result, int statusCode, Map<String, List<String>> responseHeaders) {
					sendMigrationInfoSuccessEvent(publicAddress, result);
					executorsUtil.mainThread().execute(new Runnable() {
						@Override
						public void run() {
							callback.onResponse(result);
						}
					});
				}
			});
		} catch (final ApiException e) {
			eventLogger.send(MigrationStatusCheckFailed.create(getPublicAddress(), getErrorMessage(e)));
			executorsUtil.mainThread().execute(new Runnable() {
				@Override
				public void run() {
					callback.onFailure(e);
				}
			});
		}
	}

	private String getErrorMessage(ApiException e) {
		if (e != null) {
			if (e.getResponseBody() != null) {
				return e.getResponseBody().getMessage();
			}

			return e.getMessage();
		}

		return null;
	}

	private String getPublicAddress() {
		try {
			return BlockchainSourceImpl.getInstance().getPublicAddress();
		} catch (BlockchainException e) {
			return null;
		}
	}

	private void sendMigrationInfoSuccessEvent(String publicAddress, MigrationInfo info) {
		final MigrationStatusCheckSucceeded.ShouldMigrate shouldMigrate = info.shouldMigrate() ?
			ShouldMigrate.YES : ShouldMigrate.NO;
		final MigrationStatusCheckSucceeded.IsRestorable isRstorable = info.isRestorable() ?
			IsRestorable.YES : IsRestorable.NO;
		final MigrationStatusCheckSucceeded.BlockchainVersion bcVersion =
			MigrationStatusCheckSucceeded.BlockchainVersion.fromValue(BlockchainSourceImpl.getInstance().getBlockchainVersion().getVersion());

		eventLogger.send(MigrationStatusCheckSucceeded.create(publicAddress, shouldMigrate, isRstorable, bcVersion));
	}
}
