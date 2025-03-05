use clap::{Parser, Subcommand};
use std::path::PathBuf;

#[derive(Parser)]
#[command(version, about, long_about = None)]
pub struct Cli {
    /// Sets a custom config file
    #[arg(short, long)]
    pub config: Option<PathBuf>,

    #[command(subcommand)]
    pub command: Option<Commands>,
}

#[derive(Subcommand)]
pub enum Commands {
    Server,
    CleanPictures,
    ResetPassword {
        #[arg(short, long)]
        name: String,
    },
}
