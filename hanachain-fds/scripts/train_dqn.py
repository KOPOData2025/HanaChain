"""
Script to train DQN model for fraud detection.
"""

import sys
from pathlib import Path
import pandas as pd
import argparse

# Add src to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from src.dqn_model import DQNAgent
from src.reward_function import RewardFunction
from src.trainer import DQNTrainer
from src.logger import setup_logger

logger = setup_logger(__name__, log_level="INFO", log_file="train_dqn")


def main():
    """Train DQN model."""
    # Parse arguments
    parser = argparse.ArgumentParser(description="Train DQN fraud detection model")
    parser.add_argument("--data-dir", type=str, default="data/processed",
                       help="Directory containing processed data")
    parser.add_argument("--batch-size", type=int, default=64,
                       help="Batch size for training")
    parser.add_argument("--buffer-size", type=int, default=100000,
                       help="Replay buffer size")
    parser.add_argument("--steps-per-epoch", type=int, default=1000,
                       help="Training steps per epoch")
    parser.add_argument("--checkpoint-freq", type=int, default=10,
                       help="Save checkpoint every N epochs")
    parser.add_argument("--resume", type=str, default=None,
                       help="Path to checkpoint to resume from")
    args = parser.parse_args()

    logger.info("=" * 80)
    logger.info("DQN TRAINING SCRIPT")
    logger.info("=" * 80)

    # Load training data
    logger.info("\n>>> Loading Training Data...")
    data_dir = Path(args.data_dir)
    train_df = pd.read_parquet(data_dir / "train_processed.parquet")
    logger.info(f"Loaded {len(train_df):,} training samples")
    logger.info(f"Features: {len([c for c in train_df.columns if c != 'is_fraud'])}")
    logger.info(f"Fraud rate: {train_df['is_fraud'].mean():.2%}")

    # Initialize components
    logger.info("\n>>> Initializing DQN Components...")

    # Create DQN agent
    input_dim = len([c for c in train_df.columns if c != 'is_fraud'])
    agent = DQNAgent(
        input_dim=input_dim,
        gamma=0.99,
        epsilon_start=1.0,
        epsilon_end=0.01,
        epsilon_decay_steps=10000,
        target_update_freq=100
    )
    logger.info(f"Created DQN Agent with {input_dim} input features")

    # Create reward function
    reward_fn = RewardFunction()
    logger.info("Created RewardFunction")

    # Create trainer
    trainer = DQNTrainer(
        agent=agent,
        reward_function=reward_fn,
        replay_buffer_size=args.buffer_size,
        batch_size=args.batch_size
    )
    logger.info("Created DQNTrainer")

    # Resume from checkpoint if specified
    if args.resume:
        logger.info(f"\n>>> Resuming from checkpoint: {args.resume}")
        trainer.load_checkpoint(Path(args.resume))

    # Train model
    logger.info("\n>>> Starting Training...")
    trainer.train(
        train_data=train_df,
        steps_per_epoch=args.steps_per_epoch,
        save_checkpoints=True,
        checkpoint_freq=args.checkpoint_freq
    )

    # Print summary
    logger.info("\n>>> Training Summary:")
    summary = trainer.get_metrics_summary()
    for key, value in summary.items():
        logger.info(f"  {key}: {value}")

    logger.info("\n" + "=" * 80)
    logger.info("TRAINING COMPLETE")
    logger.info("=" * 80)


if __name__ == "__main__":
    main()
